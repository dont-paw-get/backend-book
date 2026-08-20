# HANDOFF (세션별 서술 로그, append-only)

## 2026-08-18

`AGENTS.md`가 다른 프로젝트(Aiverse, MySQL, Jira 무관 feature 브랜치, `backend` 하위 모듈)의 흔적을 그대로 담고 있어서, 사용자와 대화하며 이 저장소(Book Service, Java/Spring, PostgreSQL 예정)에 맞게 하나씩 정책을 확정했다.

확정한 내용:
- DB는 PostgreSQL 단일 기준, H2 완전 제거 (Python RAG 서비스는 별도 PostgreSQL+pgvector 소유)
- `test`/`integrationTest` 분리, `check`가 둘 다 실행
- `.harness` 6개 문서 체계 도입, 계획 절차 예외 정의
- 브랜치 `{티켓번호}-{설명}`, `develop` 기준 PR, 사용자 병합, 커밋은 루트 `README.md`(CLIAR-20) 컨벤션 그대로
- JPA/테스트 기반 클래스 원칙은 문서화하되 아직 없는 클래스는 "필요 시 생성"으로 표기

`AGENTS.md` 전체를 재작성하고 `.harness/{ARCHITECTURE,STATE,PLAN,DECISIONS,HANDOFF,BACKLOG}.md`를 새로 생성했다. 상세 이유는 `DECISIONS.md`, 남은 작업은 `PLAN.md` 참조.

아직 하지 않은 것: `build.gradle`/`application.yaml`의 실제 PostgreSQL·Testcontainers 반영, `integrationTest` Gradle 태스크 구성, `.kiro` 삭제로 사라진 product/domain/architecture 스티어링 문서를 다시 만들지 여부 결정. 커밋도 아직 하지 않았다 — 현재 브랜치(`CLIAR-9-Steering-Scaffolding`) 위에서 파일만 생성된 상태이며 사용자 확인 후 커밋할 것.

다음 세션 시작 시: 이 파일 다음으로 `STATE.md`, `ARCHITECTURE.md`, `PLAN.md` 순서로 읽고, PostgreSQL 전환 작업을 계획할지 사용자에게 먼저 확인한다.

## 2026-08-18 (계속): PostgreSQL 전환 구현 (CLIAR-26)

`CLIAR-26-DB-PostgreSQL-전환` 브랜치(이미 체크아웃되어 있었음)에서 `.harness/PLAN.md`의 "PostgreSQL 전환" 체크리스트를 실행했다. H2 제거, PostgreSQL/Flyway/Testcontainers 의존성 추가, `application.yaml`+`application-local.yaml`+`application-prod.yaml` 프로필 분리, `db/migration/V1__init.sql` baseline, `docker-compose.yml`을 추가했고, H2 제거로 기존 `DpgbApplicationTests`가 DB 없이 못 뜨는 문제 때문에 "Gradle 통합 테스트 태스크" 섹션(원래 별도 계획)도 같이 처리해 `integrationTest` source set/task, `TestcontainersConfiguration`, `IntegrationTestSupport`를 만들고 그 테스트를 옮겼다. 이유와 상세는 `DECISIONS.md`(2026-08-18, "PostgreSQL 전환 — Flyway 채택...") 참조.

작업 중 발견한 문제와 우회: `io.spring.dependency-management`가 Spring Boot 4.1.0 BOM의 testcontainers-bom 중첩 import를 반영하지 못했고, 그 BOM이 가리키는 testcontainers 2.0.5도 아직 Maven Central에 없어서, 실재하는 1.21.3을 `build.gradle`에 직접 고정했다(주석으로 이유 남김). Boot/Testcontainers 업그레이드 시 재검토 필요 — `.harness/BACKLOG.md`에는 아직 안 옮겼음, 다음 세션에서 필요하면 옮길 것.

로컬에 Docker Desktop이 설치는 되어 있었지만 데몬이 꺼져 있어 직접 기동시킨 뒤 `./gradlew test`(NO-SOURCE 통과), `./gradlew integrationTest`(실제 Postgres 컨테이너로 Flyway+Spring 컨텍스트 기동 성공), `./gradlew check`까지 모두 통과를 확인했다. 커밋은 아직 하지 않았다 — 사용자 확인 후 커밋할 것.

다음 세션 시작 시: `.harness/PLAN.md`의 다음 섹션("인증 기반")을 계획할지 사용자에게 먼저 확인한다.

## 2026-08-19: 인증 기반 구현 (CLIAR-28)

`CLIAR-27` 브랜치(더 이상 할 일이 없어 로컬/원격 삭제)를 정리한 뒤, `CLIAR-28-인증-기반` 브랜치(이미 존재·체크아웃되어 있었음)에서 PLAN을 구체화하고 구현했다. 사용자와 대화하며 인증 서비스가 AWS Cognito User Pool임을 확인했고, issuer-uri 방식·`token_use`/`client_id` 커스텀 검증(Cognito Access Token에 `aud`가 없어서)·`sub`=memberId를 결정했다. PLAN.md를 먼저 완성해 커밋(`56064cc`)한 뒤 구현을 시작했다 — 사용자가 "PLAN 먼저 완성한 뒤 커밋을 하자"고 명시적으로 요청했음.

구현: `com.chc.dpgb.security` 패키지(`SecurityConfig`, `JwtAuthenticationEntryPoint`, `MemberIdResolver`, `jwt.TokenUseValidator`, `jwt.ClientIdValidator`)와 `com.chc.dpgb.common.ErrorResponse`. `build.gradle`에 `spring-boot-starter-oauth2-resource-server`/`spring-security-test` 추가, `application.yaml`에 `issuer-uri`/`app-client-id`(둘 다 `AUTH_*` env var, 기본값 없음) 추가.

구현 중 겪은 문제와 해결(전부 `.harness/DECISIONS.md`에 상세 기록):
1. Boot 4.1.0에서 Jackson 패키지가 `com.fasterxml.jackson.databind`→`tools.jackson.databind`로, `@WebMvcTest`가 `org.springframework.boot.webmvc.test.autoconfigure`로 이동 — 컴파일 에러로 발견해 import 수정.
2. `@WebMvcTest(controllers = X.class)`만으로는 테스트 클래스 내부 nested `@RestController`가 실제 등록되지 않아(원인 미확정) `@Import`로 명시 추가해 우회.
3. **가장 중요한 문제**: 실제 Cognito User Pool이 없어 `AUTH_ISSUER_URI`가 비어있는데, `JwtDecoder` 빈이 생성 시점에 OIDC discovery 네트워크 호출을 해서 `./gradlew integrationTest`의 기존 스모크 테스트(`DpgbApplicationTests`)가 깨지는 걸 미리 발견하고 고쳤다 — `JwtDecoder` 빈과 그 주입 지점(`securityFilterChain`의 파라미터) 양쪽에 `@Lazy`를 붙여야 실제로 지연되는 것을 확인(빈 정의에만 붙이면 `@Bean` 팩토리 메서드 파라미터 해석 시 여전히 즉시 생성됨). `./gradlew integrationTest`(Docker 기동 상태)로 `AUTH_ISSUER_URI` 없이도 컨텍스트가 정상 기동하는 것을 재확인했다.

검증: `./gradlew test`(신규 단위 테스트 포함 전부 통과), `./gradlew integrationTest`(실제 Postgres 컨테이너 + Security 설정 포함 컨텍스트 기동 통과) 둘 다 확인. `.harness/PLAN.md`에서 "인증 기반" 섹션 제거, `STATE.md`에 단계 요약 반영, `BACKLOG.md`에 "실제 Cognito 연동 재검증"과 "M2M 인증 필요해지면 재설계" 항목 추가.

커밋 여부는 아직 사용자에게 확인받지 않음 — 다음 행동 전에 물어볼 것.

다음 세션 시작 시: 이 구현이 커밋됐는지 git log로 확인하고, 안 됐다면 사용자에게 커밋 의사를 먼저 확인한다. 그다음 `.harness/PLAN.md`의 다음 섹션("공통 계약 인프라" 또는 "LibraryBook 도메인/영속성")을 계획할지 사용자에게 확인한다.
