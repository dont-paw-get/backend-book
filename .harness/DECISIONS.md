# DECISIONS (결정 이력, 최신이 위)

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
