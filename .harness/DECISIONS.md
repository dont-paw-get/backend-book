# DECISIONS (결정 이력, 최신이 위)

## 2026-09-02: Prometheus 메트릭 노출 — 별도 관리 포트, dev 한정 (관측-인프라-연동)

- **infra 알림(HTTP 5xx 에러율·p99 레이턴시)이 이 서비스의 Micrometer 메트릭을 필요로 한다.** `spring-boot-starter-actuator` + `micrometer-registry-prometheus`를 추가하고 `/actuator/prometheus`를 노출한다. `percentiles-histogram.http.server.requests: true`로 `http_server_requests_seconds_bucket`을 생성해야 p99 알림이 동작하고, 공통 태그 `application=backend-book`으로 infra 쿼리가 이 서비스를 특정한다(`OTEL_SERVICE_NAME`·로그 `service` 필드와 같은 값 — RCA Agent가 메트릭↔로그↔트레이스를 상관분석하는 키).
- **메인 포트(8080)가 아니라 별도 관리 포트(8081)로 서비스한다.** ALB Ingress가 `path: /`로 8080 전체를 인터넷에 노출하므로, 메인 포트에 `/actuator/prometheus`를 열면 메트릭이 외부에서 조회된다. `MANAGEMENT_SERVER_PORT=8081`로 분리하면 관리 포트는 별도 컨텍스트(자식)로 뜨고 앱의 `SecurityFilterChain`도 적용되지 않는다 — Prometheus 스크레이핑은 인증 없이 클러스터 내부에서만 가능해야 하므로 이 조합이 맞다. ALB는 8080만 노출한다.
- **dev overlay에서만 켠다.** 공통 `application.yaml`은 `management.endpoints.web.exposure.include`를 빈 값으로 둬 기본값(`health`)조차 노출하지 않는다. dev overlay의 `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE`/`MANAGEMENT_SERVER_PORT` env로만 opt-in하고, 관리 포트·Service metrics 포트·ServiceMonitor는 dev overlay 전용 patch/resource로 추가한다. prod overlay·base는 불변(`kubectl kustomize` diff 0으로 확인). prod에 Collector/Prometheus가 준비되면 같은 패턴을 이식한다.
- **메트릭은 ServiceMonitor(pull)로 받고 OTLP push는 계속 끈다.** infra Collector는 traces 파이프라인만 받으므로 `OTEL_METRICS_EXPORTER=none`·`OTEL_LOGS_EXPORTER=none`을 dev env에 명시했다(Spring은 이미 `management.otlp.metrics.export.enabled=false`로 동일 동작 — Collector 팀과의 계약을 문서화한 것). 메트릭 경로는 Prometheus 스크레이핑 하나로 유지한다.
- **`/actuator`를 `ObservationPredicate` 제외 목록에 추가했다.** ServiceMonitor 30초 스크레이핑이 span·`http.server.requests` 메트릭을 만들지 않게 — `/health`(프로브) 노이즈 제거(CLIAR-234)와 같은 이유. 단 `/actuator`는 별도 관리 포트라 `SecurityConfig`의 `permitAll` 목록에는 넣지 않는다(그 목록은 메인 8080 기준).

## 2026-09-02: 프로브·문서 경로를 서버 관측에서 제외 (CLIAR-234)

- **`ObservationPredicate` 빈으로 `/health`·`/docs`·`/webjars`·`/openapi.yaml`를 서버 span·메트릭 대상에서 뺀다.** k8s readiness(10초)·liveness(20초) 프로브와 ALB healthcheck가 전부 `GET /health`를 때리는데, Spring MVC 자동 계측은 모든 inbound 요청에 span을 만든다. dev는 sampling 1.0이라 Tempo가 프로브 trace로만 채워져 실제 요청 trace를 찾기 어려웠다.
- **sampling을 낮추는 대안은 버렸다.** `/health` 노이즈는 줄지만 실제 비즈니스 요청 trace도 같은 비율로 잃는다. OpenTelemetry rule-based sampler로 경로별 0%를 주는 방법도 있으나 설정이 복잡하고 "수동 계측 최소화" 원칙과 맞지 않는다. `ObservationPredicate`는 Boot가 `ObservationRegistry`에 자동 등록하는 표준 확장점이고, false 반환 시 Observation이 no-op이 되어 span·메트릭이 함께 빠진다.
- **제외 경로는 `SecurityConfig`의 `permitAll` 목록과 동일하다.** 둘 다 "비즈니스 트래픽이 아닌 인프라/문서 경로"라는 같은 기준이라 목록이 갈라지지 않게 맞춘다. 잃는 것은 정적 헬스 엔드포인트의 관측 데이터뿐이며, 프로브 실패는 k8s Events·pod restart로, ALB 헬스는 타깃그룹 지표로 드러난다.

## 2026-09-01: 관측 스택을 Spring Boot 4.1 네이티브 OpenTelemetry로 구성 (CLIAR-200)

- **OpenTelemetry Java Agent(`-javaagent`) 대신 Boot 네이티브 스택을 쓴다.** 처음에는 JDBC까지 한 번에 잡아주는 에이전트가 유력했지만, 실제로 Gradle로 해석해 보니 Spring Boot 4.1.0에 `spring-boot-starter-opentelemetry`가 있고 `spring-boot-opentelemetry`의 `OpenTelemetryEnvironmentVariableEnvironmentPostProcessor`가 **`OTEL_SERVICE_NAME`/`OTEL_RESOURCE_ATTRIBUTES`/`OTEL_EXPORTER_OTLP_ENDPOINT`/`OTEL_EXPORTER_OTLP_PROTOCOL`/`OTEL_TRACES_SAMPLER`/`OTEL_SDK_DISABLED` 등 OTel 표준 환경변수를 그대로 읽는다**(클래스 상수로 확인, `OTEL_EXPORTER_OTLP_ENDPOINT` 뒤에 `v1/traces`를 자동으로 붙이는 것까지). 즉 "환경변수로 설정한다"는 요구를 에이전트 없이 만족한다. 에이전트를 얹으면 Micrometer Observation과 **이중 계측**이 되고, 이미지에 25MB+ 다운로드 단계가 붙으며, 계측 동작이 Boot 자동구성 바깥에 숨는다. 얻는 것 대비 잃는 것이 컸다.
- **JDBC만 서드파티를 쓴다(`net.ttddyy.observation:datasource-micrometer-spring-boot:2.2.1`).** Micrometer Observation은 Spring MVC와 RestClient는 계측하지만 JDBC는 계측하지 않아, 요구사항의 "JDBC/PostgreSQL 구간"을 채울 방법이 이것뿐이었다. 이 저장소는 Boot 4의 모듈 세분화에 두 번(Flyway, RestClient) 데인 적이 있어 **버전 라인을 먼저 검증**했다 — 1.x는 Boot 3용이고 2.x가 Boot 4 라인이며, 2.2.1의 pom이 `spring-boot-*:4.0.5`에 컴파일되어 있다. 실제 PostgreSQL 통합 테스트 28건과 실행 중 앱의 condition report(`DataSourceObservationAutoConfiguration matched`)로 동작을 확인했다.
- **JSON 로그 포맷터를 직접 구현했다(`JsonLogFormatter`).** Boot 내장 `ecs`/`logstash` 포맷은 코드 0줄이지만 필드명이 `@timestamp`/`log.level`/`trace.id`처럼 각 규격을 따른다. 이 서비스의 로그 계약은 `timestamp`/`level`/`service`/`logger`/`message`/`trace_id`/`span_id`로 고정되어 있고 **Loki 쿼리가 그 이름에 의존**하므로, 규격 이름을 쓰고 나중에 rename 규칙을 덧붙이는 것보다 필드명을 소유하는 클래스를 하나 두는 편이 낫다고 봤다. 외부 인코더(`logstash-logback-encoder`)를 들이지 않고 Boot의 `StructuredLogFormatter` + `JsonWriter`만 썼다 — 의존성이 늘지 않고 `logback-spring.xml`도 필요 없다.
- **트레이스 export는 기본이 "꺼짐"이고 prod 프로필에서만 켠다.** 반대로(기본 켜짐 + 로컬에서 끄기) 하면 Collector 없는 로컬·CI에서 export 실패가 계속 쌓인다. 끈 상태에서도 traceId/spanId 생성과 MDC 주입은 계속 동작하므로 로컬에서도 trace_id로 로그를 묶어 볼 수 있다 — "관측을 끈 것"이 아니라 "내보내기만 끈 것"이다.
- **직접 만든 span은 2개로 제한했다.** 자동 계측이 이미 설명하는 구간에 수동 span을 얹으면 trace만 시끄러워진다. 넣은 두 개는 **자동 계측이 원인을 설명하지 못하는 곳**이다 — `book.discovery.search`(서재에 이미 있으면 외부 호출 자체가 없어, 외부 span의 유무가 이유 없이 갈린다)와 `library.shelf.rebalance`(평범한 등록 요청이 갑자기 책장 전체를 다시 저장한다). 각각 outcome/book_count 속성으로 "왜"를 남긴다. 책 수는 값 종류가 많아 메트릭 태그가 되지 않도록 high cardinality로 넣었다.
- **로그는 이벤트 중심으로만 넣고 컨트롤러에는 넣지 않았다.** 이 저장소에는 원래 로깅 코드가 한 줄도 없었기 때문에 "전부 넣기"의 유혹이 컸다. 넣은 곳은 예상하지 못한 예외(500), 외부 API 실패, 데이터 정합성 경합(등록/사서 획득 unique 충돌, 기본 책장 동시 생성, 랭크 소진), 중요한 상태 변경(책 등록·삭제, 책장 삭제, 사서 획득·대표 교체)뿐이다. **4xx는 남기지 않는다** — 계약상 정상 응답이고, 남기면 클라이언트 실수만으로 로그가 오염된다.
- **민감정보는 코드 차원에서 막았다.** `AladinBookDiscoveryClient`의 요청 URI에는 TTBKey가 쿼리스트링으로 들어 있어 **URI를 로그하지 않는다는 것을 클래스 주석으로 못 박았다**(실패 로그에는 isbn과 알라딘 errorCode/errorMessage만). JDBC span도 `jdbc.datasource-proxy.include-parameter-values: false`를 명시했다 — 라이브러리 기본값도 false지만, 기본값에 기대는 것과 정책으로 고정하는 것은 다르다.
- **Collector 주소는 관례 기본값 + `TODO:` 주석으로 뒀다.** 이 저장소는 실제 Collector Service 이름/네임스페이스를 모른다. 값을 비워 두면 배포 시 조용히 실패하고, 아무 값이나 넣으면 확정된 것처럼 보인다. 기존 prod overlay가 이미 쓰고 있는 `TODO:` 주석 컨벤션을 그대로 따라 "교체 대상"임을 표시했다.


## 2026-08-31: 신뢰하는 Cognito App Client를 FE 앱 → backend-auth Backend App Client로 이동 (CLIAR-188)

- **2026-08-19 결정의 전제가 바뀌었다.** 당시에는 "Book Service는 웹앱 하나에서만 호출된다"는 전제로 `client_id` 검증 대상을 **프론트엔드 App Client**로 잡았다(이 문서 아래 2026-08-19 항목). 그러나 최종 인증 구조는 프론트엔드가 Cognito와 직접 로그인하지 않는다 — `POST /api/v1/auth/login` → backend-auth → Cognito **Backend App Client** 순으로 발급된 Access Token이 그대로 `Authorization: Bearer`로 Book Service에 온다. backend-auth는 CLIAR-162 Phase 7에서 FE App Client 설정 자체를 코드·ConfigMap에서 제거하고 Backend App Client 하나로 통일했다. 따라서 Book Service가 신뢰해야 하는 App Client도 그쪽 하나다.
- **증상은 "설정 하나 때문에 전량 401"이었다.** 검증 로직(issuer, `token_use==access`, `client_id`, `aud` 미검증)은 이미 요구 사항과 일치했고, `AUTH_APP_CLIENT_ID` 값만 폐기된 FE App Client를 가리키고 있었다. 서명·issuer·`token_use`를 모두 통과한 뒤 마지막 `client_id`에서 떨어지는 형태라 로그만 봐서는 원인이 인증 로직처럼 보이기 쉽다.
- **과도기 dual-accept(구 FE + 신 Backend 동시 허용)를 두지 않는다.** 폐기된 App Client를 계속 신뢰하면 그 클라이언트로 발급된 토큰이 살아있는 동안 우회 경로가 남는다. backend-auth가 이미 단일 App Client로 전환을 끝냈으므로 과도기를 만들 이유도 없다. 최종 상태는 Backend App Client 단일 허용이다.
- **설정 이름은 바꾸지 않는다(`AUTH_APP_CLIENT_ID` 유지).** 이미 특정 클라이언트 종류를 함의하지 않는 중립적 이름이고, 코드(`book-service.security.cognito.app-client-id`) ↔ `application.yaml` ↔ ConfigMap 3단이 일관돼 있다. rename은 세 곳을 동시에 바꾸면서 얻는 게 주석으로 대체 가능한 명확성뿐이라 하지 않았다. 대신 dev·prod overlay 주석에 "backend-auth의 `COGNITO_BACKEND_CLIENT_ID`와 반드시 같아야 한다"는 대응 관계를 적었다. App Client ID는 비밀값이 아니므로 계속 ConfigMap에 둔다(Client Secret은 backend-auth만 갖고 Book Service는 쓰지 않는다).
- **검증 체인을 `CognitoAccessTokenValidator`로 뽑았다.** 기존에는 `SecurityConfig.jwtDecoder` 안에서 `DelegatingOAuth2TokenValidator`를 인라인으로 조합했는데, `SecurityConfigTest`가 `JwtDecoder`를 `@MockitoBean`으로 대체하므로 **그 조합을 지나는 테스트가 하나도 없었다**. validator 3개를 한 클래스로 묶어 프로덕션과 테스트가 같은 객체를 쓰게 했다 — 테스트가 배선의 복제본이 아니라 실제 배선을 검증한다. 테스트는 로컬 RSA 키쌍으로 토큰을 서명하고 `NimbusJwtDecoder.withPublicKey`로 디코딩해, Cognito JWKS/discovery 네트워크 호출 없이 결정론적으로 돈다.
- **prod overlay도 같은 값으로 맞췄다.** prod는 현재 dev User Pool을 공용 중이라(아래 2026-08-27 관련 항목) 값이 같은 것이 정합적이다. 다만 backend-auth prod overlay에는 아직 `COGNITO_BACKEND_CLIENT_ID`가 없고 placeholder만 있어, 상용 전용 User Pool 전환 시 양쪽을 같은 작업에서 맞춰야 한다(`BACKLOG.md`).

## 2026-08-30 (계속): prod EKS 엔드포인트 공개 접근 유지, prod DB 비밀번호 교체

- **prod EKS 클러스터 엔드포인트를 `publicAccessCidrs: 0.0.0.0/0`으로 유지한다.** 열려 있는 것은 Kubernetes API 서버(컨트롤 플레인)이고 서비스 트래픽 경로(ALB → 파드)와는 무관하다. 모든 요청은 AWS IAM 인증과 Kubernetes RBAC 두 관문을 통과해야 하므로 "누구나 조작 가능"한 상태는 아니다 — 자물쇠는 잠겨 있고 문 앞까지 올 수 있는 상태다. **감수하는 위험은 명확하다**: 공격 표면이 인터넷에 노출되고, IAM 자격증명이 유출되면 위치 제한 없이 즉시 악용된다. CIDR 제한이 대안이지만 팀이 고정 IP를 쓰지 않으면 재택·이동 때마다 목록을 고쳐야 해 실효가 없고, 잘못 적용하면 전원이 클러스터에서 잠긴다. private 전용 전환은 VPN·Bastion 등 접근 수단을 새로 만들어야 해 현 규모에 과하다. 팀의 고정 IP 정책이 생기면 재검토한다.
- **prod Aurora `admin` 비밀번호를 32자로 교체했다.** 최초 설정 과정에서 PowerShell 인용 문제로 실패한 `kubectl patch`의 에러 메시지에 평문이 출력돼 노출된 것이 이유다. 교체는 마스터 유저 없이 했다 — PostgreSQL에서 역할은 자기 비밀번호를 스스로 바꿀 수 있으므로 `admin`으로 접속해 `ALTER ROLE admin PASSWORD`를 실행했다. 값이 출력·argv·셸 히스토리 어디에도 남지 않도록 stdin 전달과 `kubectl patch --patch-file`을 썼다. **Windows에서 `kubectl patch -p`에 JSON을 넘기는 방식은 앞으로 쓰지 않는다** — PowerShell 5.1이 네이티브 실행 파일에 인자를 넘길 때 큰따옴표를 벗겨내 `ConvertTo-Json` 결과도 `\"` 이스케이프도 상황에 따라 깨진다. `--patch-file` 또는 bash 경유가 확실하다.

## 2026-08-30: prod 인프라 결정 3건 — NAT Gateway 신설, Aurora Serverless v2 전환, DB 분리 방향 (CLIAR-112 후속)

- **NAT Gateway를 둔다(단일 AZ, `public2-ap-northeast-2b`).** prod private 서브넷에 인터넷 아웃바운드가 아예 없어 외부 API(알라딘) 호출이 구조적으로 불가능했다. "백엔드는 private 서브넷에 둔다"는 원칙과 NAT는 충돌하지 않는다 — NAT는 아웃바운드 전용이라 인바운드 연결을 허용하지 않는다. 나가지도 못하게 막힌 상태는 보안이 아니라 기능 결손이었다. AZ는 현재 book 노드가 있는 2b를 골라 AZ 간 데이터 전송을 피했다. **AZ 장애 시 전체 아웃바운드가 끊기는 것은 알면서 감수**한 것이며, 런칭 시점에 AZ별 NAT 2개로 늘리고 각 private 라우팅 테이블이 같은 AZ의 NAT를 가리키게 한다(`BACKLOG.md`).
- **`logs`/`sts`/`secretsmanager` 인터페이스 엔드포인트는 추가하지 않는다.** 처음에는 NAT 데이터 처리료 절감을 위해 권장했으나 계산해 보니 역전이었다 — 인터페이스 엔드포인트는 AZ마다 ENI가 상시 과금되는데(3종 × 2AZ = 6 ENI), 절감 대상인 로그·시크릿 트래픽은 작은 JSON 수준이라 고정비가 절감액을 크게 웃돈다. 기존 S3·ecr.api·ecr.dkr 엔드포인트는 이미지가 165MB로 커서 정당하므로 유지한다. 트래픽이 실제로 커지면 재검토한다.
- **Aurora를 Serverless v2로 전환한다(0.5~8 ACU), reader 삭제, 스토리지 Standard.** 7일 실측이 커넥션 0, CPU 6~7%, 데이터 52MB, I/O 월 약 520만 건이었는데 `db.r7g.large` 2대 + I/O-Optimized로 운영되고 있었다. Serverless v2는 ACU 단가가 provisioned 동급보다 비싸 **평균 사용률이 동급 클래스의 50~60%를 넘으면 오히려 손해**지만, 현 사용률은 그 구간에서 한참 아래다. I/O-Optimized는 I/O 요금이 전체의 25%를 넘을 때 유리한 옵션인데 실제 I/O 비용은 월 $1~2 수준이라 명백한 손해였다. reader는 커넥션 0·읽기 IOPS 하루 2.4로 읽기 분산 수요가 없어 삭제했고, **writer 장애 시 페일오버가 수십 초에서 수 분으로 느려지는 것을 감수**한 것이다 — 런칭 전 다시 붙인다(`BACKLOG.md`). 최소 용량을 0이 아니라 0.5 ACU로 둔 것은, 자동 일시정지의 콜드 스타트(십수 초)가 배포 검증 중 원인 규명을 헷갈리게 만들 수 있어서다.
- **DB 분리는 "같은 클러스터 안에서 데이터베이스 분리"로 간다(클러스터 분리 아님).** PostgreSQL은 데이터베이스 간 JOIN이 `dblink`/`postgres_fdw` 없이 불가능하므로, `CLAUDE.md`가 요구하는 "다른 서비스의 schema를 직접 JOIN해서 조회할 수 없다"를 엔진이 강제해 준다. 클러스터·인스턴스를 늘리지 않으니 추가 비용이 0이다. Aurora에서 인스턴스는 스토리지를 공유하므로 **인스턴스를 늘려도 DB가 나뉘지 않는다** — 격리 수단이 아니라 HA/읽기 확장 수단이다. 클러스터 분리(완전 격리)는 필요해지면 그때 Serverless v2로 가고, 그때도 인스턴스 고정비가 배수로 늘지 않는다. 다만 `CLAUDE.md`는 "자신만의 PostgreSQL 인스턴스·데이터베이스를 소유"라고 인스턴스까지 명시하고 있어, 이 결정은 문서를 엄격히 읽으면 부분 충족이다 — 문서 문구 조정 여부는 미결(`BACKLOG.md`).
- **실행 시점은 prod 배포 마무리 이후로 미룬다.** auth·record 테이블을 옮기면 다른 팀 서비스가 dev에서 즉시 중단되고 각 저장소의 `DATABASE_URL`도 같은 시점에 바꿔야 해서, 진행 중인 장애 대응에 끼워 넣을 크기가 아니다. 계획만 `PLAN.md`에 확정해 두고 팀 확인을 기다린다.

## 2026-08-30: 컨테이너 이미지를 멀티아키(amd64+arm64)로 빌드 — prod arm64 노드 CrashLoopBackOff 해결 (CLIAR-112)

- **배경:** `dpyb-prod`의 `backend-book` 파드가 이틀 넘게 CrashLoopBackOff(exit 255, 550회 재시작)였다. 컨테이너 로그는 `exec /opt/java/openjdk/bin/java: exec format error` 한 줄뿐 — JVM이 기동조차 못 했다는 뜻이라 애플리케이션/설정 문제가 아니었다. CI가 `ubuntu-latest`(amd64)에서 `docker build`를 돌려 **단일 아키텍처 amd64** 이미지만 만들고 있었고(ECR 매니페스트가 manifest list가 아닌 `manifest.v2`로 확인), `dpyb-prod` 노드는 전부 arm64(Graviton)다.
- **dev가 멀쩡했던 것은 우연이다:** `dpyb-dev`는 amd64/arm64 혼합이고 dev overlay에 `nodeSelector`가 없어 파드가 amd64 노드에 착지했을 뿐이다. Karpenter consolidation·노드 교체·amd64 여유 부족 중 하나만 걸려도 dev도 같은 증상으로 죽는다 — 즉 이것은 prod만의 문제가 아니라 **양쪽에 걸린 잠재 결함**이었다.
- **결정: 멀티아키 이미지로 전환한다.** `Dockerfile`의 빌드 스테이지를 `--platform=$BUILDPLATFORM`으로 러너에 고정하고, CI를 `docker/setup-buildx-action` + `docker/build-push-action`(`platforms: linux/amd64,linux/arm64`)으로 교체했다. Java jar이 아키텍처 중립이라 Gradle 빌드는 여전히 한 번만 돌고, 런타임 스테이지에는 `RUN`이 없어 QEMU 에뮬레이션 실행이 발생하지 않는다 — 멀티아키 비용이 사실상 0인 이유.
- **채택하지 않은 대안 1 — prod NodePool을 amd64로 고정**(`nodepool-book.yaml`에 `kubernetes.io/arch In ["amd64"]`): 이미지 변경 없이 가장 빠르지만 prod 클러스터 전체가 Graviton인 비용 설계를 backend-book만 되돌리는 셈이고, 단일 아키텍처 이미지라는 근본 원인과 dev의 잠재 결함이 그대로 남는다. **긴급 롤백 레버로만 남겨둔다.**
- **채택하지 않은 대안 2 — arm64 단독 빌드**: dev 클러스터가 혼합 아키텍처라 dev 파드가 amd64 노드에 스케줄되면 방향만 뒤집힌 같은 장애가 난다.
- **부수 결정:** `provenance: false`. buildx 기본값은 provenance attestation을 붙여 ECR 이미지 목록에 `unknown/unknown` 항목이 생기는데, 이를 꺼서 매니페스트가 아키텍처 2개로만 보이게 했다.
- **영향받은 산출물:** `Dockerfile`, `.github/workflows/build-push-ecr.yml`, `.harness/ARCHITECTURE.md`(배포 절 — 노드 아키텍처 현황·멀티아키 빌드·Dockerfile 2-스테이지 구조), `.harness/STATE.md`. `k8s/`·`argocd/` 매니페스트는 변경하지 않았다(문제가 배포 정의가 아니라 이미지에 있었으므로).

## 2026-08-25 (계속): DB 스키마 대개편 구현 중 발견한 기술적 결정 — soft delete 구현 방식, totalPages nullable 동작, LibrarianLevel 엔티티 보류

- **soft delete를 `@SQLRestriction`으로 구현:** 서비스/리포지토리마다 `deleted_at IS NULL` 조건을 반복하지 않고, `Shelf`/`LibraryBook`/`Scrap`/`Librarian` 엔티티 클래스에 Hibernate `@SQLRestriction("deleted_at IS NULL")`(6.3+, 구 `@Where` 대체)을 붙여 모든 조회(파생 쿼리·JPQL·`findById` 포함)에 자동 적용되게 했다. 하드 `delete()` 포트 메서드는 4개 aggregate 모두 제거하고, 서비스가 `entity.softDelete(Instant.now()); repository.save(entity);`로 통일했다.
- **`LibraryBook.totalPages` nullable 전환에 따른 도메인 규칙 확장:** DB가 `total_pages`를 nullable로 바꾼 것을 그대로 따라 `totalPages`를 `Integer`로, `progress()`가 `totalPages`가 없으면 `null`을 반환하도록 확장했다. 이 판단의 근거는 이미 `.harness/DOMAIN.md`에 있던 서술("알라딘 API가 totalPages를 대부분 제공하지 않아 사용자가 직접 입력해야 하는 것이 일반적인 경로")이 실제로는 여전히 필수값으로 강제되고 있던 모순을 해소한 것 — 별도 확인 없이 스키마 변경의 자연스러운 연장으로 판단해 진행했다.
- **`ReadingStatus` 값 재정의:** ADR-0005가 제거했던 것과 값 구성이 다르다(`PLANNED`/`READING`/`COMPLETED`, 구버전은 `NOT_STARTED`/`READING`/`COMPLETED`) — 사용자가 제공한 SQL의 정확한 값을 그대로 따랐다. 독립 필드로 두고 `progress`와의 자동 연동 로직은 만들지 않았다(스키마/CRUD 범위 확정과 일치).
- **`LibrarianLevel` 엔티티는 만들지 않음(YAGNI):** DB 테이블(`librarian_level`)과 FK 제약은 V9 마이그레이션으로 만들었지만, 레벨업 로직이 이번 범위 밖이라 앱 코드 어디서도 레벨 정책 값을 조회하지 않는다. JPA 엔티티 없이 순수 DB 제약으로만 남겨뒀다 — 레벨업 API를 실제로 만들 때 추가.
- **네이티브 Postgres enum 매핑 패턴 확립:** `genre_type`/`book_reading_status`/`librarian_type` 3종 모두 `@Enumerated(EnumType.STRING) + @JdbcTypeCode(SqlTypes.NAMED_ENUM)`(Hibernate 6.2+)으로 매핑했다 — 이 저장소 최초의 네이티브 enum 컬럼 사용 사례. 이후 네이티브 enum 컬럼을 추가할 때 이 패턴을 따른다.
- **`CLIAR-45`의 "책 삭제 시 스크랩 cascade" 통합 테스트 제거:** `ON DELETE CASCADE`가 V8에서 제거되면서 그 테스트의 전제 자체가 사라졌다. DB 레벨 cascade 검증 대신, 캐스케이드는 이제 `LibraryBookService.deleteLibraryBook` → `ScrapService.softDeleteAllByBookId` 오케스트레이션으로 이동했고, 이는 `LibraryBookServiceTest`/`ScrapServiceTest`(Mockito 단위 테스트)로 검증한다.
- **영향받은 문서:** `.harness/STATE.md`(구현 완료 단계 반영). `docs/api/openapi.yaml`/`DOMAIN.md`/`ARCHITECTURE.md`/신규 ADR/`BACKLOG.md`는 아직 이 구현을 반영하지 않았다 — `.harness/PLAN.md` 참조.

## 2026-08-25: DB 스키마 대개편 방향 확정 — genre/reading_status 재도입, librarian 소유 모델 전면 개편(ADR-0009 대체), soft delete 전 aggregate 도입

- **배경:** 사용자가 `shelf`/`library_book`/`scrap`/`librarian`(+신규 `librarian_level`/`librarian_type_info`)을 아우르는 확정 SQL을 제공했다. 스키마 자체(PK `id` 통일, `member_id` UUID화, 전 aggregate `deleted_at` soft delete, `genre`/`reading_status` 컬럼 재도입, `librarian`의 마스터→회원 소유 인스턴스 전환)는 이 SQL을 그대로 소스로 삼기로 확정했다.
- **작업 범위 확정:** 이번 개편은 스키마/엔티티/CRUD까지만이다. 경험치 획득 트리거·레벨업 시점 부수효과 같은 게임 로직 설계는 범위 밖 — `level`/`experience` 컬럼은 갖되 그 값을 바꾸는 비즈니스 규칙은 이번에 만들지 않는다.
- **ADR-0009 대체 확정:** `is_representative`가 Book Service의 `librarian` 테이블에 재도입되면서, 대표 사서 선택·조회를 Book Service가 다시 소유하는 것으로 확정했다. ADR-0009(대표 사서 선택을 Member 서비스로 이관, CLIAR-46 결정의 반전)를 이번 결정이 다시 반전시키는 것이므로, 구현 시 새 ADR 번호로 대체 기록하고 ADR-0009에도 "이 결정은 ADR-00xx로 반전됨" 각주를 남긴다(아직 코드/ADR 문서 반영 전 — 방향만 확정).
- **`librarian.name` 확정:** 회원이 사서를 획득/등록할 때 직접 이름을 짓는다 — 서버가 타입 마스터(`librarian_type_info`) 이름을 복사해 채우지 않는다(애초에 `librarian_type_info`에는 이름 필드가 없음).
- **`librarian_level` 시드 범위 확정:** 이번엔 `level=1, required_experience=0` 최소치만 시드하고, 나머지 레벨 정책 값은 미정 상태로 `.harness/BACKLOG.md`에 이연한다.
- **`evolution_stage` 컬럼 폐기 확정:** 기존 `librarian`(마스터 카탈로그) 테이블에 있던 필드였지만, 신규 SQL(`librarian`도 `librarian_type_info`도)에는 없다 — 이 개념 자체를 제거하는 것으로 간주한다.
- **기술적 이슈 발견:** 사용자가 제공한 SQL은 `librarian`을 `librarian_type_info`보다 먼저 `CREATE TABLE`하면서 그 테이블을 참조하는 FK(`type librarian_type NOT NULL REFERENCES librarian_type_info (type)`)를 걸고 있어 순서상 오류다 — 실제 Flyway 마이그레이션에서는 `librarian_type` enum → `librarian_type_info` → `librarian_level` → `librarian` 순으로 재배열해야 한다.
- **영향받은 문서:** `.harness/PLAN.md`(설계 논의 서술을 TODO 체크리스트로 재정리). 실제 구현은 아직 시작 전 — Flyway 마이그레이션, 엔티티/서비스/컨트롤러, `docs/api/openapi.yaml`, `docs/db/erd.dbml`, `.harness/DOMAIN.md`/`ARCHITECTURE.md`, 신규 ADR, `.harness/BACKLOG.md`는 아래 2026-08-25(계속) 결정이 정리된 뒤 착수한다.

## 2026-08-25 (계속): DB 스키마 대개편 남은 결정 사항 확정 — API 설계·개명·삭제 API·마이그레이션 분할·soft delete 부수효과

- **librarian API 엔드포인트 설계 확정:** `.harness/PLAN.md`에 제안했던 구조 그대로 진행 — `GET /api/v1/librarian-types`(타입 카탈로그, 기존 `getLibrarians` 대체), `POST /api/v1/librarians`(사서 획득, `type`+`name` 필수, 타입별 1마리 제약 409), `GET /api/v1/librarians`(내 보유 목록), `PATCH /api/v1/librarians/{id}`(이름 변경), `PATCH /api/v1/librarians/{id}/representative`(대표 지정), `GET /api/v1/librarians/representative`(대표 조회), `DELETE /api/v1/librarians/{id}`(방출, 아래 참조).
- **사서 개명(이름 변경) 허용 확정:** 언제든 `PATCH /api/v1/librarians/{id}`로 이름을 바꿀 수 있다 — 등록 시 1회 고정이 아니라 다른 aggregate와 동일한 CRUD 관례를 따른다.
- **사서 삭제(방출) API 확정:** 이번 범위에 포함한다. `DELETE /api/v1/librarians/{id}`가 soft delete(`deleted_at`)로 처리한다 — 하드 삭제 아님, 다른 aggregate(Shelf/LibraryBook/Scrap)와 동일한 soft delete 정책을 따른다.
- **Flyway 마이그레이션 분할 확정:** 하나로 묶지 않고 aggregate별로 여러 파일로 분리한다 — 예: `V7__rescope_shelf_and_library_book.sql`(shelf/library_book PK·UUID·soft delete·genre/reading_status), `V8__rescope_scrap.sql`(scrap PK·soft delete·scrap_image_url), `V9__redesign_librarian.sql`(librarian_type enum·librarian_type_info·librarian_level·librarian 전면 개편) — 각 단계를 독립적으로 검증/롤백할 수 있게 한다. 정확한 파일명은 구현 시점에 재확인.
- **soft delete 부수효과 기본안 확정:** 제안대로 진행 — 전 조회 쿼리에 `deleted_at IS NULL` 일괄 적용. Shelf 삭제는 소속 LibraryBook 전부를 기본 책장으로 이동시킨 뒤 그 Shelf 행만 soft delete(기존 하드 삭제 동작 유지, soft delete로만 전환). LibraryBook soft delete 시 소속 Scrap 전체를 애플리케이션이 벌크로 soft delete(기존 DB `ON DELETE CASCADE`를 대체).
- **영향받은 문서:** `.harness/PLAN.md`에서 "우선순위 1" 섹션이 모두 해소되어 제거되고 구현 체크리스트만 남았다.

## 2026-08-21: DB 정책 재반전 — MSA 원칙에 맞게 서비스별 PostgreSQL 분리로 되돌림

- **기존 결정 재반전:** 2026-08-20에 "Java 기반 MSA 서비스 전체가 PostgreSQL 인스턴스·데이터베이스 하나를 공유하고 서로의 schema를 직접 JOIN할 수 있다"로 정책을 바꿨었다(당시도 사용자 명시적 지시). 이번에 사용자가 "MSA 의의에 맞게" 서비스마다 DB를 분리하는 쪽으로 다시 명시적으로 방향을 바꿨다 — database-per-service가 서비스 간 결합도를 낮추는 MSA 본래 취지에 더 부합한다는 판단.
- **되돌린 내용:** Book Service는 다시 자신만의 PostgreSQL 인스턴스·데이터베이스만 소유한다. 다른 Java MSA 서비스든 Python RAG 서비스든 schema를 직접 JOIN할 수 없고, 모든 서비스 간 데이터 공유는 API 또는 event로만 한다(RAG는 애초에 이 정책 변경 전후 내내 별도 DB를 유지해 영향 없음).
- **실제 코드/설정에는 영향이 없었다:** 확인해보니 `docker-compose.yml`(`POSTGRES_DB: dpgb`), `application-local.yaml`(`jdbc:postgresql://localhost:5432/dpgb`), `application-prod.yaml`(`DB_URL`/`DB_USERNAME`/`DB_PASSWORD` env var)은 애초부터 이 저장소 전용 DB(`dpgb`)만 가리키고 있었다 — 공유 DB 정책은 "다른 서비스의 schema를 같은 인스턴스에서 직접 JOIN할 수 있다"는 향후 가능성만 문서화했을 뿐, 실제로 공유할 다른 서비스의 schema/테이블 이름이 정해진 적도, JOIN 쿼리가 작성된 적도 없었다(2026-08-20 결정문 자체가 이를 "아직 하지 않은 것"으로 명시). 그래서 이번 되돌림은 문서(정책 서술)만 수정하면 되고 코드/마이그레이션/설정 변경은 없다.
- **영향받은 문서:** `AGENTS.md`/`CLAUDE.md`(하네스: DB 정책 — 공유 서술 제거, database-per-service로 재기술), `.harness/ARCHITECTURE.md`(서비스 경계), `.harness/BACKLOG.md`(공유 DB 전제의 백로그 항목 — 스키마/계정 분리, DB 이름 재검토 — 제거, 더 이상 해당 없음).

## 2026-08-21: Scrap CRUD API 구현 — 책 삭제 시 스크랩 cascade 삭제, 실제 구현 티켓은 CLIAR-45 (CLIAR-45)

- **티켓 번호 정정:** `.harness/PLAN.md`는 계약 설계 당시 티켓(CLIAR-43, API 계약 재정의)을 그대로 라벨로 남겨뒀었다. 실제 구현은 별도 티켓 `CLIAR-45-Scrap-CRUD-API` 브랜치에서 진행됐다 — Shelf가 "계약 설계(CLIAR-47) → 구현(CLIAR-32)"로 티켓이 나뉘었던 것과 같은 패턴.
- **책 삭제 시 스크랩 cascade 삭제:** `.harness/DOMAIN.md`가 스크랩을 "LibraryBook을 통해서만 귀속되는 하위 리소스"로 규정하지만, 소유 책이 삭제됐을 때 스크랩을 어떻게 할지는 명시하지 않았다. Shelf 삭제 시 책을 기본 책장으로 "이동"시키는 것과 달리 스크랩은 옮길 곳이 없으므로, `V4__create_scrap.sql`의 `book_id` FK에 `ON DELETE CASCADE`를 걸어 책이 삭제되면 스크랩도 함께 삭제되도록 DB 레벨에서 처리했다. 애플리케이션 코드(`LibraryBookService.deleteLibraryBook`)는 스크랩을 전혀 알 필요가 없다.
- **스크랩은 독립 `memberId`를 저장하지 않는다:** DOMAIN.md 원칙을 그대로 따라 `scrap` 테이블에 `member_id` 컬럼을 두지 않았다. 대신 스크랩 스코프 endpoint(`getScrap`/`updateScrap`/`deleteScrap`)는 매번 스크랩 → 소속 `LibraryBook` 조회를 거쳐 소유권을 검증한다(조회 1회 추가, 하지만 aggregate 경계를 코드로도 강제할 수 있는 이점).
- **테스트 함정 — DB cascade와 Hibernate 1차 캐시 불일치:** `ScrapRepositoryTest`에서 `libraryBookRepository.delete(book)` + `flush()`만으로는 DB가 cascade로 지운 스크랩 행을 Hibernate 영속성 컨텍스트가 여전히 "관리 중"으로 착각해 `scrapRepository.findById(...)`가 여전히 값을 반환했다. `TestEntityManager.clear()`(패키지가 Boot 4.1에서 `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`로 재배치됨)로 1차 캐시를 비운 뒤에야 실제 DB 상태를 재확인할 수 있었다.
- 영향받은 문서: `.harness/DOMAIN.md`(Scrap aggregate 절에 cascade 규칙 추가), `.harness/ARCHITECTURE.md`(패키지 구조·마이그레이션·테스트 목록), `.harness/PLAN.md`(Scrap CRUD API 섹션 제거), `.harness/STATE.md`.

## 2026-08-21: 자바 코드 스타일 확정 — 줄바꿈된 파라미터 목록도 연속 들여쓰기(8칸) 그대로 유지

- **배경:** `docs/intellij-java-wooteco-style.xml`로 전체 자바 코드(`src` 하위 98개 파일)를 재포맷한 뒤, 생성자/메서드 파라미터가 120열을 넘겨 줄바꿈될 때 파라미터 줄이 8칸(연속 들여쓰기) 들여써지는 게 맞는지 문제 제기가 있었다. 사용자는 `ALPHABET` 예시처럼 "표현식이 길어 줄바꿈"하는 경우만 8칸이고, 파라미터를 한 줄에 하나씩 나열하는 "일반적인 경우"는 4칸(기본 블록 들여쓰기)이어야 하지 않냐고 물었다.
- **확인된 사실:** IntelliJ Java 코드 스타일 모델은 `INDENT_SIZE`(블록)와 `CONTINUATION_INDENT_SIZE`(그 외 모든 줄바꿈) 두 값만 제공하고, "파라미터 목록 전용 들여쓰기"라는 별도 옵션이 없다. `METHOD_PARAMETERS_WRAP`/`CALL_PARAMETERS_WRAP` 등은 줄바꿈 여부만 결정할 뿐 칸수는 전부 `CONTINUATION_INDENT_SIZE`(현재 8)를 공유한다. 즉 표현식 줄바꿈과 파라미터 목록 줄바꿈을 이 xml만으로는 서로 다른 칸수로 분리할 수 없다 — 분리하려면 값 자체를 바꿔 다른 컨텍스트에도 영향을 주거나(Option B), 괄호 위치 정렬(가변 칸수, Option C)로 우회하거나, IntelliJ 포맷터 밖의 커스텀 도구를 새로 만들어야 한다. 참고로 Google Java Format(레퍼런스 구현)도 파라미터 목록에 예외를 두지 않고 동일한 2배 들여쓰기 원칙을 적용한다.
- **결정:** Option A(현재 유지) 확정 — 표현식 줄바꿈과 파라미터 목록 줄바꿈 모두 `CONTINUATION_INDENT_SIZE=8`을 그대로 적용한다. 사용자가 대안들의 트레이드오프(Option B는 표현식 줄바꿈도 4칸이 되어 4.5.2 규칙과 충돌·문서 수정 필요, Option C는 메서드/생성자 이름 길이에 따라 정렬 칸수가 가변적이라 diff가 들쭉날쭉해짐)를 확인한 뒤 현재 상태 유지를 선택했다.
- **영향:** 코드/문서 변경 없음 — 이미 적용된 `docs/intellij-java-wooteco-style.xml`/`docs/JAVA_STYLE_GUIDE.md`와 그 결과로 재포맷된 전체 자바 코드가 그대로 최종 상태다. `.harness/PLAN.md`의 논의 섹션 제거, `.harness/STATE.md`에 한 줄 반영.

## 2026-08-21: Book Discovery API — 스텁 대신 실제 알라딘 연동, 라이브 호출로 응답 형태 확정 (CLIAR-34)

- **계획 변경:** `.harness/PLAN.md`는 원래 "자격 증명이 없으므로 어댑터 인터페이스 + 스텁 구현"을 계획했었다. 구현 도중 사용자가 실제 알라딘 TTBKey(`.env`의 `ALADIN_API_TTB_KEY`)를 확보했다고 알려와, 스텁을 만들지 않고 바로 실제 연동(`AladinBookDiscoveryClient`)을 구현했다.
- **실제 응답 형태를 라이브 호출로 확인:** 문서만으로는 알 수 없던 세 가지를 실제 알라딘 API를 호출해 확인했다. (1) 오류도 HTTP 200으로 오고 바디가 `{"errorCode":..,"errorMessage":..}` 형태다 — HTTP status가 아니라 응답 바디의 `errorCode` 존재 여부로 실패를 판정해야 한다. (2) `OptResult=itemPage`를 요청해도 `subInfo.itemPage`는 테스트한 모든 검색에서 비어 있었다 — "totalPages는 대부분 없다"(2026-08-20 결정)는 실측으로도 확인됐다. (3) `isbn13`은 항상 신뢰 가능한 13자리 숫자지만 `isbn`(10자리) 필드는 "K"로 시작하는 알라딘 내부 코드인 경우가 있어, `isbn13`을 우선 사용하고 유효성(정규식) 검사 후 실패하면만 `isbn`으로 폴백한다.
- **QueryType 라우팅:** 알라딘 ItemSearch는 `Query`+`QueryType` 한 쌍만 받고 title/author를 동시에 AND 검색하는 기능이 없다. `title`만 있으면 `QueryType=Title`, `author`만 있으면 `QueryType=Author`로 정밀 검색하고, 둘 다 있으면 `QueryType=Keyword`(자유 검색)로 두 값을 공백으로 이어붙여 보낸다.
- **`spring-boot-starter-restclient` 추가:** `RestClient.Builder`가 `spring-boot-starter-webmvc`만으로는 자동구성되지 않아(Boot 4.1 세분화 모듈 체계, `spring-boot-starter-flyway` 때와 같은 패턴) 별도로 추가했다. 빠뜨린 채로 통합 테스트를 돌려 `NoSuchBeanDefinitionException`으로 바로 드러났다.
- **`@Lazy`를 빈+주입 지점 양쪽에:** `AladinBookDiscoveryClient`(`@Value`로 TTBKey를 읽는 빈)에 `@Lazy`만 붙이고 `BookDiscoveryService` 생성자 주입 지점에는 붙이지 않았더니, `ALADIN_API_TTB_KEY`가 없는 환경(이 세션의 실행 셸 포함, CI도 마찬가지)에서 `./gradlew integrationTest`가 즉시 실패했다 — CLIAR-28(`JwtDecoder`)에서 이미 겪었던 것과 똑같은 원인이라 같은 해법(양쪽 모두 `@Lazy`)을 적용했다.
- **테스트 전략:** 실제 네트워크 호출 없이 `MockRestServiceServer`에 라이브 호출로 캡처한 실제 응답 JSON을 fixture로 사용해 매핑·에러·QueryType 분기를 검증했다 — 반복 가능하고 자격 증명에 의존하지 않는 테스트를 유지하면서도 실제 응답 형태를 정확히 반영한다.
- **미해결:** `.env`는 이 앱이 자동으로 읽지 않는다(dotenv 미도입) — 사용자가 로컬 실행 시 직접 셸/IDE에 `ALADIN_API_TTB_KEY`를 주입해야 한다. 필요해지면 dotenv 도입 여부를 별도로 검토(`.harness/BACKLOG.md` 후보).
- 영향받은 문서: `.harness/ARCHITECTURE.md`(기술 스택·저장소 구조·비밀값 절), `.harness/STATE.md`, `.harness/PLAN.md`(Book Discovery API 섹션 제거), `build.gradle`.

## 2026-08-20: `spring-boot-starter-flyway` 누락 발견 및 추가 (CLIAR-31)

- **문제:** LibraryBook 도메인/영속성(CLIAR-31) 구현으로 이 저장소 최초의 `@Entity`(`LibraryBook`)와 Flyway migration(`V2__create_library_book.sql`)을 추가하자, `RepositoryIntegrationTestSupport`(`@DataJpaTest`)와 `@SpringBootTest`(`IntegrationTestSupport`) 양쪽에서 Hibernate가 `ddl-auto: validate` 단계에서 `missing table [library_book]`로 실패했다.
- **원인:** CLIAR-26에서 PostgreSQL/Flyway를 도입할 때 `org.flywaydb:flyway-core`/`flyway-database-postgresql`(순수 Flyway 라이브러리)만 추가했고, Spring Boot 4.1의 autoconfigure 모듈(`org.springframework.boot:spring-boot-flyway`, `FlywayAutoConfiguration` 포함)은 별도 `spring-boot-starter-flyway`로만 제공된다는 점을 놓쳤다. 그 결과 Flyway가 앱 기동 시 한 번도 자동 실행되지 않았지만, `V1__init.sql`이 빈 baseline이고 엔티티가 없어 검증할 테이블이 없었던 탓에 CLIAR-26 당시의 `./gradlew integrationTest` 스모크 테스트는 이 결함을 드러내지 못했다.
- **조치:** `build.gradle`에 `implementation 'org.springframework.boot:spring-boot-starter-flyway'`를 추가했다. 또한 `@DataJpaTest`의 큐레이션된 autoconfiguration 목록(`DataJpaRepositoriesAutoConfiguration`, `HibernateJpaAutoConfiguration`만 포함) 자체가 Flyway를 배제하므로, `RepositoryIntegrationTestSupport`에 `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`를 명시적으로 추가했다(`@SpringBootTest` 기반 `IntegrationTestSupport`는 전체 autoconfiguration을 로드하므로 이 문제가 없다).
- **검증:** `./gradlew test`/`integrationTest`/`check` 모두 통과, `LibraryBookRepositoryTest`(unique 제약 위반 포함)로 실제 스키마 생성을 확인했다.
- 영향받은 문서: `.harness/ARCHITECTURE.md`(기술 스택·저장소 구조), `.harness/STATE.md`.

## 2026-08-20: DB 정책 반전 — Java MSA 서비스 전체가 PostgreSQL 하나를 공유, 직접 JOIN 허용 (CLIAR-43)

- **기존 결정 반전:** 2026-08-18에 "Book Service와 Python RAG Service는 각자 PostgreSQL을 소유하고 DB를 직접 공유하지 않는다"고 결정했었다. 사용자가 "MSA로 서버는 여러 개지만 RDB는 하나만 사용해서 각 서비스가 원하는 데이터를 조인해서 사용하기로 했다"고 명시적으로 방향을 바꿔, Java 기반 MSA 서비스 전체가 PostgreSQL 인스턴스·데이터베이스 하나를 공유하고 서로의 schema를 직접 JOIN할 수 있도록 정책을 바꿨다.
- **Python RAG 서비스는 예외:** 이 공유 DB에 RAG 서비스는 포함되지 않는다 — RAG는 지금처럼 자체 PostgreSQL + pgvector를 별도로 소유하고, 데이터 공유는 여전히 API/event로만 한다. 사용자에게 직접 확인해 RAG는 범위에서 제외했다.
- **schema 소유권은 유지:** 하나의 DB를 공유하더라도 각 서비스는 자신의 schema(테이블)를 자신의 Flyway migration으로만 관리한다. 다른 서비스 schema는 읽기용 JOIN 대상일 뿐, 쓰기 마이그레이션 권한은 옮기지 않는다.
- **이 저장소에서 아직 하지 않은 것:** 실제로 공유할 다른 서비스의 schema/테이블 이름, JOIN이 필요한 구체적 쿼리, DB 계정·권한 분리 방식은 아직 정해지지 않았다 — 다른 서비스가 구체화되는 시점에 재검토.
- 영향받은 문서: `AGENTS.md`/`CLAUDE.md`(하네스: DB 정책), `.harness/ARCHITECTURE.md`(서비스 경계). `docker-compose.yml`/`application-*.yaml`은 이 저장소가 이미 단일 PostgreSQL에 연결하는 구조라 즉시 변경할 부분은 없었다.

## 2026-08-20: API 계약 재정의 — 장르/무드/language 제거, 알라딘 단일 소스화, 신규 리소스 2종 추가 (CLIAR-43)

- 상세 배경과 결정 목록은 `docs/api/decisions/0003-scope-narrowing-and-new-resources.md`(ADR-0003) 참조 — API wire 계약 결정은 `docs/api`가 소유하므로 이 문서에는 요약만 남긴다.
- 핵심: 장르(`genre`)·무드(`moodTags`)·`language` 완전 제거, 표지 OCR·AI 도서 분석 엔드포인트 삭제, 외부 도서 검색을 알라딘 API 단일 소스로 한정, 스크랩(Scrap)·동물 사서(Librarian)를 신규 리소스로 추가.
- **이후 반전됨 — 장르(`genre`)만:** 2026-08-25 결정(DB 스키마 대개편, 위 참조)으로 `genre` 제거만 다시 반전되어 `library_book`에 재도입됐다. `moodTags`/`language` 제거, OCR·AI 엔드포인트 삭제, 알라딘 단일 소스화, Scrap/Librarian 신규 편입 등 이 결정의 나머지는 그대로 유효하다.
- **기존 결정 반전 1 — 스크랩 범위:** `docs/api/decisions/0002-library-book-schema-fixes.md`가 "문장 OCR·감상·비밀 메모는 다른 MSA 컴포넌트 담당이라 범위 밖"이라고 명시했던 것을, 사용자가 담당 기능표를 다시 확인하면서 스크랩 CRUD를 이 저장소 범위로 재편입하는 것으로 뒤집었다. 문장을 이미지에서 추출하는 OCR 자체(텍스트 인식)는 여전히 범위 밖이다.
- **기존 결정 반전 2 — `language`:** 같은 ADR-0002가 "사용자가 선택 입력, 생략 시 서버가 `ko`로 채운다"로 도입했던 `language` 필드를, 알라딘 API가 언어 정보를 전혀 제공하지 않고 담당 기능표에도 없어 전 스키마·필터에서 제거했다.
- **알라딘 API 실제 응답 확인:** 사용자가 제공한 실제 알라딘 API 예시로 두 가지를 확인했다. (1) `totalPages`(페이지 수)는 대부분의 도서에서 응답에 아예 없다 — 선택 필드로 유지하고, 사용자 직접 입력이 예외가 아니라 일반 경로임을 문서에 명시했다. (2) `author`는 "이름 (지은이)" 형식의 역할 라벨이 붙은 결합 문자열이라, 서버가 역할 라벨을 제거하고 이름만(여러 명이면 쉼표로 구분) 반환하도록 정했다 — 원문 그대로 저장하면 저자 필터·정렬·중복 판정이 깨지기 때문. 파싱 로직은 아직 구현 전이며 `.harness/PLAN.md`의 Book Discovery API 섹션에 체크리스트로 남겼다.
- **이미지 파일 업로드 기능 추가 후 제거:** 같은 작업에서 표지 이미지 교체(`replaceLibraryBookCover`)와 스크랩 이미지 교체(`replaceScrapImage`)를 한 차례 신규 리소스로 추가했으나, 둘 다 오브젝트 스토리지(S3 등) 연동이 필요해 "단순 DB CRUD" 범위를 벗어난다는 걸 뒤늦게 확인해 사용자 확인 후 제거했다. `coverUrl`은 문자열(URL) 필드로만 남아 등록/수정 요청에서 계속 설정할 수 있다.

## 2026-08-19: 인증 기반 — AWS Cognito 대상 Resource Server 설정, 실제 Pool 없이도 기동 가능하게 구성

- **인증 서비스 = AWS Cognito User Pool:** 사용자 확인. `issuer-uri` 형식은 `https://cognito-idp.{region}.amazonaws.com/{userPoolId}`.
- **App Client가 사실상 1개:** Book Service는 웹앱 하나(모바일도 웹뷰로 동일 웹앱)에서만 호출된다는 사용자 확인에 따라, `client_id` 검증(웹앱 App Client 제한)을 인증 기반 작업에서 바로 포함시켰다. 다른 백엔드 MSA 컴포넌트가 M2M으로 직접 호출하는 시나리오는 지금 다루지 않는다 — 필요해지면 별도 검토.
- **Cognito Access Token은 `aud` 클레임이 없다:** 표준 OIDC의 audience 검증(Spring `audiences` 옵션)을 쓸 수 없어, 대신 `token_use`(ID Token 거부) + `client_id`(등록된 App Client 제한) 커스텀 `OAuth2TokenValidator` 2개로 대체했다(`com.chc.dpgb.security.jwt.TokenUseValidator`, `ClientIdValidator`).
- **memberId = `sub` 클레임:** Cognito `sub`는 불변 UUID라 회원 식별자로 적합하다고 판단, 추출 로직은 `com.chc.dpgb.security.MemberIdResolver` 한 곳에 모았다.
- **`JwtDecoder` 빈과 그 주입 지점을 모두 `@Lazy`로 표시:** issuer-uri 기반 `JwtDecoder`는 생성 시점에 OIDC discovery 네트워크 호출을 한다. 실제 Cognito User Pool이 아직 없어 `AUTH_ISSUER_URI`를 비워둔 상태인데, `@Lazy`를 빈 정의에만 붙이면 `securityFilterChain` 빈이 생성자 인자로 `JwtDecoder`를 요구하면서 여전히 즉시 생성되는 문제가 있어(Spring이 `@Bean` 팩토리 메서드 파라미터 해석 시 지연 프록시를 자동으로 안 만듦), 주입 지점 파라미터에도 `@Lazy`를 추가로 붙여야 실제로 지연됐다. `./gradlew integrationTest`(`AUTH_ISSUER_URI` 미설정 상태)로 컨텍스트가 정상 기동하는 것을 확인했다. Book Discovery 어댑터와 같은 "자격 증명 없을 때 스텁으로 격리" 원칙의 연장선.
- **Spring Boot 4.1.0의 패키지 이동 두 가지 확인:** (1) Jackson이 `com.fasterxml.jackson.databind`가 아니라 `tools.jackson.databind`(Jackson 3, `spring-boot-starter-jackson`이 끌어옴)로 바뀌었다. (2) `@WebMvcTest`가 `org.springframework.boot.test.autoconfigure.web.servlet`이 아니라 `org.springframework.boot.webmvc.test.autoconfigure`(`spring-boot-webmvc-test` 모듈)로 이동했다. `ARCHITECTURE.md` 기술 스택에 반영.
- **`@WebMvcTest(controllers = X.class)`만으로는 테스트 클래스 내부 nested `@RestController`가 실제로 등록되지 않았다:** 원인은 확정하지 못했으나(Boot 4.1의 컴포넌트 스캔 경계 변경 추정), `@Import({SecurityConfig.class, X.class})`로 nested 컨트롤러를 명시적으로 같이 import해서 우회했다. 이후 도메인 컨트롤러가 생기면 이 패턴이 여전히 필요한지 재확인.

## 2026-08-18: PostgreSQL 전환 — Flyway 채택, `integrationTest` 태스크·기반 클래스 동시 구성

- **Flyway 채택:** `spring.jpa.hibernate.ddl-auto`를 `validate`로 고정하고 스키마 변경은 Flyway migration(`src/main/resources/db/migration`)으로만 한다. 이유: 운영 DB 스키마를 Hibernate 자동 생성에 맡기지 않고 명시적 이력으로 남기는 편이 이 팀의 ADR 중심 작업 방식(`docs/api/decisions/`)과 일관된다. 아직 도메인 엔티티가 없어 최초 `V1__init.sql`은 빈 baseline이며, 첫 테이블은 `LibraryBook` aggregate 구현과 함께 다음 migration에서 추가한다.
- **`integrationTest` 태스크를 PostgreSQL 전환과 같은 작업에서 구성:** `.harness/PLAN.md`는 원래 "PostgreSQL 전환"과 "Gradle 통합 테스트 태스크"를 별도 섹션으로 분리해뒀지만, H2를 제거하고 나면 기존 `DpgbApplicationTests`(`@SpringBootTest`)가 실제 DB 없이는 컨텍스트를 못 띄운다. `ARCHITECTURE.md`가 이미 "datasource/Flyway 도입 시 이 스위트를 옮긴다"고 명시해뒀으므로, `integrationTest` source set/task, `TestcontainersConfiguration`, `IntegrationTestSupport`를 함께 만들고 `DpgbApplicationTests`를 `src/integrationTest`로 옮겼다. `RepositoryIntegrationTestSupport`(`@DataJpaTest`)는 아직 Repository가 없으므로 만들지 않고 보류했다 — "필요해지면 만든다" 원칙 유지.
- **Testcontainers 버전 직접 고정:** `io.spring.dependency-management`(1.1.7)가 `spring-boot-dependencies`(4.1.0)의 `testcontainers-bom` 중첩 import를 반영하지 못해 `org.testcontainers:junit-jupiter`/`postgresql` 버전을 못 찾는 문제가 있었고, Boot 4.1.0이 가리키는 `testcontainers.version=2.0.5`는 Maven Central에 아직 게시되지 않은 상태였다(직접 조회로 확인, 현재 Central 최신은 1.21.3). 두 문제를 동시에 우회하기 위해 `build.gradle`에 실재하는 1.21.3을 직접 명시했다. Boot 업그레이드나 Testcontainers 2.x 정식 게시 시 재검토 필요.
- **로컬 Postgres는 `docker-compose.yml`로 제공:** PLAN 체크리스트에는 없었지만 `AGENTS.md`/DB 정책이 "로컬은 Docker로 PostgreSQL 실행"을 못박고 있고, `application-local.yaml` 기본값(계정 `dpgb`/`dpgb`, 5432 포트)이 이 파일과 짝을 이뤄야 의미가 있어 함께 추가했다.
- **검증:** Docker Desktop을 로컬에서 기동해 `./gradlew test`(NO-SOURCE 통과), `./gradlew integrationTest`(실제 Postgres 컨테이너 기동 + Flyway + Spring 컨텍스트 기동 성공), `./gradlew check`(둘 다 실행) 모두 통과를 실제로 확인했다.

## 2026-08-18: AGENTS.md를 Book Service에 맞게 재작성하고 `.harness` 체계 도입

- **DB:** Book Service는 전용 PostgreSQL만 사용한다. H2(`com.h2database:h2`, `spring-boot-h2console`)는 제거한다. 이유: RAG 벡터 검색을 위해 별도 Python 서비스가 PostgreSQL+pgvector를 쓰기로 했고, 팀 전체가 하나의 DB 엔진을 공유하면 운영 지식과 로컬 환경을 통일할 수 있다. H2와 PostgreSQL의 SQL 방언·제약조건·동시성 차이가 크고, 사용자별 중복 방지·동시 등록 같은 이 서비스의 핵심 불변식은 PostgreSQL로만 신뢰성 있게 검증 가능하다.
- **서비스 경계:** Book Service(이 저장소, Java)와 Python RAG Service는 각자 PostgreSQL을 소유하고 DB를 직접 공유하지 않는다. 데이터 교환은 API 또는 event로 한다.
- **테스트 태스크 분리:** 빠른 `test`(DB 없음)와 PostgreSQL Testcontainers 기반 `integrationTest`를 분리한다. `check`와 CI는 항상 둘 다 실행한다. 이유: 로컬 반복 개발 속도와 PostgreSQL 실제 검증을 모두 확보하기 위함.
- **`.harness` 도입:** Claude Code/Codex/Kiro를 번갈아 사용할 예정이므로 `HANDOFF/STATE/ARCHITECTURE/PLAN/DECISIONS/BACKLOG` 6개 문서로 크로스 툴 연속성을 관리한다. `.kiro/steering`은 삭제된 상태이며 API 계약 산출물(`docs/api/*`)만 계속 별도로 소유한다.
- **계획 절차의 예외:** 파일을 바꾸지 않는 설명/조사/리뷰와 명백한 소규모 수정은 `PLAN.md` 초안 없이 즉시 수행한다. 기능·DB·API·아키텍처 변경만 계획 승인을 거친다. 이유: 매 요청에 계획 절차를 강제하면 단순 질문까지 느려진다.
- **브랜치/커밋:** 브랜치명은 `{티켓번호}-{설명}` 형식으로 통일하고 기존 `feature/{번호}-{한글}` 규칙은 폐기한다. `develop`에서 분기해 PR을 `develop`으로 생성하고 사용자가 병합한다. 커밋 컨벤션은 저장소 `README.md`(`CLIAR-20`)의 실제 팀 규칙(영어 타입 + 한국어 제목, `[티켓번호]`, scope)을 그대로 따른다. 문서 전용 작업도 티켓 브랜치에서 진행하며 main 직접 커밋 예외는 두지 않는다. 자동 병합·삭제·push는 하지 않는다. 이유: 기존 `AGENTS.md`가 이 프로젝트가 아닌 다른 프로젝트(Aiverse, MySQL, Jira 무관 feature 브랜치)의 흔적이었고, 실제 Git 이력(`CLIAR-*` 커밋, `main`/`develop` 브랜치, README 컨벤션)과 맞지 않았다.
- **JPA/테스트 기반 클래스:** N+1 방지, fetch join, DTO 분리 원칙은 지금부터 문서화하지만, 아직 존재하지 않는 `RepositoryIntegrationTestSupport`/`IntegrationTestSupport` 같은 기반 클래스는 "필요해지면 만든다"로 서술하고 이미 존재하는 것처럼 쓰지 않는다. 이유: 아직 entity/Repository가 없는 초기 골격 단계에서 존재하지 않는 클래스를 실재하는 것처럼 서술하면 다른 도구가 잘못된 전제로 코드를 찾게 된다.
