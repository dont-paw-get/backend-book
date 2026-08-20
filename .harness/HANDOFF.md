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

## 2026-08-20: CLIAR-28 커밋 + API 계약 재정의 착수

`CLIAR-28-인증-기반` 브랜치에 남아있던 인증 구현을 사용자 확인 후 커밋(`461e2b1`, `feat(security): [CLIAR-28] Cognito 기반 JWT 인증 구현`)했다. `origin/CLIAR-28-인증-기반` 대비 2 커밋 ahead 상태이며 아직 push는 하지 않았다.

이어서 사용자가 `docs/api/openapi.yaml` 범위를 크게 재정의하는 요청을 시작했다(장르·무드 추출 제거, 알라딘 API 기준으로만 응답 필드 구성, 스크랩 CRUD·동물 사서·책 표지 커스터마이징 신규 리소스 추가, OCR/AI 분석 엔드포인트 제거). 티켓 번호(CLIAR-43)를 받아 `develop`에서 브랜치를 만들려 했으나, 이미 다른 도구/세션에서 만들어둔 `CLIAR-43-기획-변경으로-인한-API-수정-및-추가` 브랜치(원격에도 존재, develop과 동일 커밋)를 발견해 그쪽으로 옮겨 작업했다.

확인이 필요했던 지점 4가지를 사용자에게 확인받았다: (1) 장르/무드는 필터에서도 완전 제거, (2) OCR·AI 분석 엔드포인트 둘 다 삭제, (3) 이미지 업로드는 JPEG/PNG/WEBP·최대 10MB, (4) 스크랩 PATCH는 `sentence`/`pageNumber`/`memo`를 항상 전부 포함하고 `null`이면 삭제하는 방식. `.harness/PLAN.md`에 설계 초안을 먼저 적고 확인받은 뒤 구현(계약 수준)을 진행했다.

완료: `docs/api/openapi.yaml` 전면 재작성(버전 0.2.0), `docs/api/decisions/0003-scope-narrowing-and-new-resources.md` 신설, `docs/api/README.md` 변경 목록 갱신, `.harness/DOMAIN.md`(범위 서술 갱신, LibraryBook에서 genre/moodTags 제거, Scrap/Librarian aggregate 규칙 신설), `.harness/PLAN.md`(신규 계약에 맞게 전체 재정리 — Scrap/Librarian 섹션 추가), `.harness/STATE.md`(CLIAR-43 완료 요약 추가, CLIAR-10 줄에 반전 사실 주석 추가). openapi.yaml은 `$ref` 무결성·중복 `operationId`·미사용 스키마/응답을 스크립트로 검증 완료(전부 통과).

아직 하지 않은 것: 실제 도메인/영속성/컨트롤러 구현(전부 `.harness/PLAN.md`에 체크리스트로만 있음), 이번 변경사항 커밋(사용자 확인 필요), 이 브랜치의 push.

## 2026-08-20 (계속): 알라딘 API 실데이터 반영 + DB 정책 반전

사용자가 알라딘 API 실제 응답 예시를 줘서 두 가지를 확정했다: (1) `totalPages`는 대부분의 응답에 아예 없다 — `docs/api/openapi.yaml`의 `ExternalBook.totalPages` 설명에 반영. (2) `author`는 "이름 (지은이)" 형식 결합 문자열이라 서버가 역할 라벨을 제거하고 이름만 반환하기로 확정 — `ExternalBook.author` 설명과 `.harness/DOMAIN.md`, `.harness/PLAN.md`(Book Discovery API 체크리스트)에 반영했다. 파싱 로직 자체는 아직 구현하지 않았다.

이어서 사용자가 DB 아키텍처를 크게 바꿨다: "MSA로 서버는 여러 개지만 RDB는 하나만 써서 각 서비스가 원하는 데이터를 조인해서 사용하기로 했다." 2026-08-18에 확정했던 "Book Service와 RAG는 각자 PostgreSQL 소유, 직접 조회 금지" 결정을 반전하는 것이라 RAG 서비스도 포함되는지 확인받았고, **RAG는 여전히 별도 PostgreSQL+pgvector를 유지하고 Java MSA 서비스들만 하나의 PostgreSQL을 공유**하는 것으로 확정했다. `AGENTS.md`·`CLAUDE.md`의 "하네스: DB 정책" 섹션(두 파일 동일하게 유지), `.harness/ARCHITECTURE.md`(서비스 경계), `.harness/DECISIONS.md`(반전 사유), `.harness/BACKLOG.md`(다른 서비스 schema/계정 구조가 정해지면 재검토할 항목)에 반영했다. `docker-compose.yml`/`application-local.yaml`은 여전히 `dpgb` 단일 DB를 가리키고 있고, 다른 서비스가 아직 구체화되지 않아 지금 바꿀 내용은 없었다 — 실제 공유 스키마/계정 구조가 나오면 재검토 필요.

커밋 여부는 아직 사용자에게 확인받지 않음.

## 2026-08-20 (계속): 이미지 파일 업로드 endpoint 2개 제거 (S3 의존)

사용자가 "표지 이미지 교체는 빼줘. S3 등록할만한 내용이 있다면 확인받고 제거해"라고 요청했다. 표지 교체(`PUT /api/v1/library/books/{bookId}/cover`)를 먼저 지우고, 같은 이유(오브젝트 스토리지 필요)로 스크랩 이미지 교체(`PUT /api/v1/library/scraps/{scrapId}/image`)도 해당되는지 확인 질문을 했더니 "이것도 제거"로 확정됐다.

`docs/api/openapi.yaml`에서 두 endpoint, `CoverImageResponse`/`ScrapImageResponse` 스키마, `InvalidCoverImageFile`/`InvalidScrapImageFile` 오류 응답, `ScrapDetailResponse.imageUrl` 필드를 전부 제거했다(검증 스크립트로 `$ref`/미사용 스키마 재확인 완료, operationId 17→15개). `.harness/DOMAIN.md`(LibraryBook `coverUrl`은 등록/수정 두 경로만 남음, Scrap에 이미지 연결 기능 없음을 명시), `.harness/PLAN.md`(두 체크리스트 항목 제거), `docs/api/decisions/0003-scope-narrowing-and-new-resources.md`(제목 수정, 결정 6/7/8/10번 추가·수정), `.harness/STATE.md`, `.harness/BACKLOG.md`(파일 저장소 연동이 필요해지면 재설계할 항목 추가)에 반영했다.

커밋 여부는 아직 사용자에게 확인받지 않음.

다음 세션 시작 시: 이번 세션에서 만든 변경사항(API 계약 재정의 + DB 정책 반전 + 이미지 업로드 제거)이 커밋됐는지 확인하고, 안 됐다면 커밋 의사를 먼저 확인한다. 그다음 `.harness/PLAN.md`의 어느 섹션(공통 계약 인프라 / LibraryBook 도메인·영속성 / Scrap / Librarian 등)부터 구현할지, 또는 다른 서비스의 공유 DB schema 구조를 먼저 정할지 사용자에게 확인한다.
