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

## 2026-08-20 (계속): 공통 계약 인프라 구현 (CLIAR-29)

이미 체크아웃되어 있던 `CLIAR-29-공통-계약-인프라` 브랜치(origin과 동기화된 상태, 이전 CLIAR-43 커밋 위에서 분기)에서 `.harness/PLAN.md`의 "공통 계약 인프라" 섹션을 구현했다. 먼저 `docs/api/openapi.yaml`의 `components.responses.*` 21개를 스캔해 stable error code가 여러 endpoint에서 재사용되는 패턴(예: `LIBRARY_BOOK_ACCESS_DENIED`는 조회/수정/삭제/진도수정 4곳 공용)을 확인하고, HTTP status별 abstract 예외 + stable code별 concrete 예외로 이루어진 2단 계층 설계를 `PLAN.md`에 먼저 적어 제시했다. 사용자에게 두 가지를 확인받았다: (1) 계약에 없는 500 fallback(`INTERNAL_ERROR`) 포함 여부 → 포함, (2) concrete 예외 13종을 한꺼번에 만들지 도메인 구현 시점마다 나눠 만들지 → 한꺼번에.

구현: `com.chc.dpgb.common.exception` 패키지에 `DomainException`(abstract, `code()` 추상 메서드) → status별 abstract 5종(`BadRequestException`/`ForbiddenException`/`NotFoundException`/`ConflictException`/`BadGatewayException`) → stable code별 concrete 13종(`InvalidSearchParameterException`, `InvalidBookDataException`, `InvalidFilterParameterException`, `InvalidPageValueException`, `InvalidScrapDataException`, `LibraryBookAccessDeniedException`, `ScrapAccessDeniedException`, `LibraryBookNotFoundException`, `ScrapNotFoundException`, `LibrarianNotSelectedException`, `LibrarianNotFoundException`, `BookAlreadyRegisteredException`, `AladinApiException`). 각 concrete 예외는 `openapi.yaml` example의 기본 메시지를 갖는 무인자 생성자와, 메시지를 덮어쓰는 생성자를 함께 제공한다(같은 code라도 endpoint마다 예시 메시지가 다른 경우 — 예: `INVALID_BOOK_DATA`의 등록 vs 수정 — 를 대비). `GlobalExceptionHandler`(`@RestControllerAdvice`)가 5개 abstract 타입 + 예기치 못한 `Exception`(500, `INTERNAL_ERROR`)을 `ErrorResponse{code, message}`로 매핑한다. 401(`UNAUTHORIZED`)은 기존 `JwtAuthenticationEntryPoint`가 Spring Security 필터 단계에서 전담하므로 건드리지 않았다.

`GlobalExceptionHandlerTest`(`@WebMvcTest`, 기존 `SecurityConfigTest`의 "테스트 전용 nested 컨트롤러 + `@Import`" 패턴 재사용, `SecurityConfig`를 함께 import하고 `.with(jwt())`로 인증 통과)로 6개 status(400/403/404/409/502/500) 매핑과 응답 body(`code`/`message`)를 검증했다. `./gradlew test` 전체 통과(신규 테스트 6개 포함) 확인.

concrete 예외는 아직 LibraryBook/Scrap/Librarian 도메인 패키지가 없어 전부 `common.exception`에 배치했다 — 실제 aggregate 구현 시점에 해당 도메인 패키지로 옮길지는 그때 재검토하기로 `PLAN.md`/`ARCHITECTURE.md`에 남겨뒀다. `PLAN.md`에서 "공통 계약 인프라" 섹션을 제거하고 `STATE.md`에 단계 요약을 반영, `ARCHITECTURE.md`의 저장소 구조·테스트 구조 절에 새 패키지/테스트를 추가했다.

커밋 여부는 아직 사용자에게 확인받지 않았다. (세션 종료 후 커밋(`d5d8079`)·PR·병합(#7, `c026011`)이 이 저장소 밖에서 진행됐다 — 다음 세션에서 확인.)

## 2026-08-20 (계속): LibraryBook 도메인/영속성 구현 (CLIAR-31)

CLIAR-29가 커밋·PR·`develop` 병합까지 완료된 상태로 이미 `CLIAR-31-LibraryBook-도메인-영속성` 브랜치가 체크아웃되어 있었다. 브랜치에는 이 세션이 시작하기 전부터 이미 다른 도구/세션이 만든 미커밋 변경(작업 트리)이 있었다 — `docs/api/openapi.yaml`을 `bookNumber`(정수)에서 `shelfRank`(LexoRank 문자열, ADR-0004)로 재설계하고 전용 `reorderLibraryBook` endpoint를 추가한 상태였다. 이를 먼저 읽고 그 계약을 기준으로 계획을 세웠다.

사용자가 곧바로 `ReadingStatus`(`NOT_STARTED`/`READING`/`COMPLETED`)를 완전히 제거하라고 지시해, 구현 전에 `docs/api/openapi.yaml`(v0.4.0)·`docs/api/decisions/0005-remove-reading-status.md`(신규 ADR)·`.harness/DOMAIN.md`("페이지와 독서 상태"→"페이지와 진도율")·`.harness/PLAN.md`를 먼저 정리했다. `$ref` 무결성·미사용 스키마·중복 operationId 검증 스크립트로 계약을 재확인했다.

구현: `com.chc.dpgb.library` 패키지에 `LibraryBook`(JPA entity, aggregate root — `register`/`updateMetadata`/`updateProgress`/`changeShelfRank`에 불변식 캡슐화, 위반 시 `IllegalArgumentException`), `ShelfRank`(LexoRank pure 유틸 — `[0-9A-Za-z]` 62자 알파벳으로 `initial`/`before`/`after`/`between`/`rebalancedSequence`, 두 이웃 사이 공간이 128자를 넘어서면 `ShelfRankExhaustedException`), `LibraryBookRepository`(소유권 스코프 조회, shelfRank 순 조회, ISBN/정규화 제목·저자 중복 확인). `ShelfRank.between`의 미드포인트 알고리즘은 "next 경계가 실제로 유효한 동안만 상한으로 쓰고, 한번 prev보다 확실히 작은 자리를 골라 분기한 뒤로는 무제한으로 취급"하는 방식으로 설계해, 두 값이 깊은 공통 접두사를 공유해도 (그리고 어떤 생성값도 알파벳의 최솟값 문자로 끝나지 않는다는 불변식 덕분에) 항상 종료됨을 직접 증명하며 만들었다. Flyway `V2__create_library_book.sql`로 `library_book` 테이블 + unique 제약(`member_id`+`shelf_rank`, `member_id`+`isbn` partial, `member_id`+정규화 제목·저자 partial, `shelf_rank`는 `COLLATE "C"`) 추가.

`RepositoryIntegrationTestSupport`(`@DataJpaTest`+Testcontainers, 이 저장소 최초 사용)를 신설하는 과정에서 실제 버그를 발견했다: Hibernate가 `missing table [library_book]`로 실패했는데, 원인은 CLIAR-26에서 `flyway-core`/`flyway-database-postgresql`만 추가하고 Boot 4.1의 autoconfigure 모듈(`spring-boot-starter-flyway`, `FlywayAutoConfiguration` 포함)을 빠뜨려서 Flyway가 앱 기동 시 한 번도 자동 실행되지 않았던 것 — `V1__init.sql`이 빈 baseline이라 그동안 드러나지 않았다. `build.gradle`에 `spring-boot-starter-flyway`를 추가하고, `@DataJpaTest`의 큐레이션된 autoconfiguration이 Flyway를 포함하지 않는다는 것도 확인해 `RepositoryIntegrationTestSupport`에 `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`를 추가했다. 상세는 `.harness/DECISIONS.md` 참조.

검증: `ShelfRankTest`(10개)·`LibraryBookTest`(11개, 단위) + `LibraryBookRepositoryTest`(9개, unique 제약 위반 포함, Testcontainers) 전부 통과, `./gradlew check`(Docker Desktop 기동 상태) 전체 통과 확인. `.harness/PLAN.md`에서 "LibraryBook 도메인/영속성" 섹션 제거, `STATE.md`에 단계 요약 반영, `ARCHITECTURE.md`(저장소 구조·기술 스택·테스트 구조) 갱신.

이번 티켓은 aggregate·값 객체·영속성까지만 다뤘다 — controller/service 배선, `shelfRank` rebalance를 언제 트리거할지(재조회+재할당+저장 오케스트레이션), 도메인 `IllegalArgumentException`을 `common.exception`의 어떤 타입(`InvalidBookDataException`/`InvalidPageValueException` 등)으로 번역할지는 모두 "Library CRUD API" 섹션으로 이연했다 — `.harness/PLAN.md`에 그 지점을 명시해뒀다.

커밋하기 전에 사용자가 `LibraryBookRepository`를 포트/어댑터로 분리하라고 추가 요청했다: "Repository(interface), RepositoryJpaImpl(implement) 이런식으로 ... JPA 쿼리 메서드와 service 단의 메서드명을 분리". 기존 `LibraryBookRepository`(Spring Data JPA 인터페이스, 파생 쿼리 메서드명)를 `LibraryBookJpaRepository`(package-private)로 이름을 바꾸고, 그 자리에 순수 인터페이스 `LibraryBookRepository`(포트 — `save`/`findOwnedBook`/`findShelfOrderedByRank`/`findLastRanked`/`existsByIsbn`/`existsByNormalizedTitleAndAuthor`)를 새로 만들고, `LibraryBookRepositoryJpaImpl`(`@Repository`)이 `LibraryBookJpaRepository`로 위임하도록 구현했다. `LibraryBookRepositoryTest`(integration)는 JPA 슬라이스 자체(flush 시점·unique 제약)를 검증하는 것이 목적이라 포트가 아니라 `LibraryBookJpaRepository`를 직접 autowire하도록 유지했다(같은 패키지라 package-private 접근 가능). `./gradlew test`/`integrationTest` 재확인 통과. `.harness/PLAN.md`(Library CRUD API의 rebalance 오케스트레이션 문구를 새 포트 메서드명으로 갱신)와 `ARCHITECTURE.md`(저장소 구조), `STATE.md`(CLIAR-31 요약에 포트/어댑터 분리 반영)에 즉시 반영했다.

이어서 사용자가 "기존의 JpaRepository도 전부 Impl로 옮겨"라고 요청해, `LibraryBookJpaRepository`를 `LibraryBookRepositoryJpaImpl` 안의 nested interface로 옮기는 시도를 했다. `./gradlew check`로 실제 검증하니 `NoSuchBeanDefinitionException`으로 전부 실패했다 — Spring Data JPA의 repository 스캐닝이 nested interface를 빈으로 등록하지 못하는 것을 확인(전체 `@SpringBootTest`(`DpgbApplicationTests`)까지 실패해 `@DataJpaTest` 슬라이스만의 문제가 아님을 확인). 원래의 top-level package-private `LibraryBookJpaRepository.java`로 되돌리고 재검증해 통과를 확인했다 — 이 구조가 이미 "패키지 밖에서는 `LibraryBookJpaRepository`에 접근 불가, `LibraryBookRepositoryJpaImpl`만 사용" 목표를 만족한다.

사용자와 포트/어댑터/JPA 프록시 구조에 대해 질의응답을 몇 차례 주고받은 뒤(Impl이 왜 JpaRepository를 직접 구현하지 않고 주입받는지, JDBC/QueryDSL이었다면 계층이 어떻게 달라지는지 등), 사용자가 마지막으로 두 가지를 요청했다: (1) `LibraryBookRepositoryJpaImpl` → `LibraryBookRepositoryJpaAdapter`로 이름 변경("Impl 대신 adapter"), (2) `LibraryBook` entity의 수동 getter 12개를 Lombok `@Getter`로 교체. 둘 다 그대로 적용하고 `./gradlew check` 재통과 확인(Lombok 어노테이션 프로세서는 이미 `build.gradle`에 있어 추가 설정 불필요). `ARCHITECTURE.md`(파일명, Lombok 기술스택 설명)와 `STATE.md`(CLIAR-31 요약)에 반영했다.

이어서 사용자가 `normalizedTitle`/`normalizedAuthor`·`Instant`·`protected` 생성자에 대해 질문했고(각각 답변: 화면용 원본과 중복판정용 정규화 값 분리, 타임존 없는 UTC 절대시각, JPA 요구사항이지 Spring Bean과 무관), 마지막으로 `updateMetadata`를 `if (x != null)` 부분 수정 대신 항상 전체 필드를 강제하도록 바꿔달라고 요청했다. 이건 `docs/api/openapi.yaml`의 `UpdateLibraryBookRequest`(`minProperties: 1`, 부분 수정 허용) 계약과 정면으로 부딪혀서, entity만 바꿀지 계약까지 같이 바꿀지 `AskUserQuestion`으로 확인했고 "계약까지 함께 바꿈"으로 답을 받았다.

`docs/api/openapi.yaml`(v0.5.0)의 `UpdateLibraryBookRequest`를 `Scrap.updateScrap`과 동일한 패턴(7개 필드 `required`, `isbn`/`publisher`/`publishedDate`/`coverUrl`는 `type: [string, "null"]`로 null=삭제, `title`/`author`/`totalPages`는 null 불가)으로 재작성하고 `docs/api/decisions/0006-update-library-book-full-payload.md`(ADR-0006) 신설. `LibraryBook.updateMetadata(...)`도 같은 규칙으로 다시 구현(부분 수정 null-스킵 로직 제거, title/author null이면 즉시 거부, isbn/publisher/publishedDate/coverUrl는 무조건 대입해 null이면 실제로 지워지게, totalPages는 `Integer`→`int`로 필수화). `LibraryBookTest`의 관련 테스트를 새 계약에 맞게 다시 쓰고(4개, 총 14개) `./gradlew check` 재통과 확인. `.harness/DOMAIN.md`(LibraryBook aggregate 절 + "결정된 사항 (ADR-0006 반영)"), `.harness/PLAN.md`(Library CRUD API의 updateLibraryBook 체크리스트 문구), `docs/api/README.md`(ADR 목록), `.harness/STATE.md`에 반영했다.

이어서 사용자가 `normalizedTitle`/`normalizedAuthor` 기반 중복 판정 자체를 없애달라고 요청했다 — "다른 책이어도 제목과 저자가 같을 수 있어서 사용자 자율로 두고 싶다"는 이유. `openapi.yaml`을 먼저 확인해보니 이 메커니즘은 애초에 wire 계약 문면에 노출된 적이 없어(그냥 `BookAlreadyRegistered` 409로만 서술) 스키마 변경은 불필요했다. `LibraryBook`에서 `normalizedTitle`/`normalizedAuthor` 필드·`normalize()` 메서드·관련 대입 로직을 전부 제거하고, `LibraryBookRepository`(포트)/`LibraryBookJpaRepository`/`LibraryBookRepositoryJpaAdapter`에서 `existsBy...NormalizedTitleAndNormalizedAuthor` 계열 메서드를 제거, `V2__create_library_book.sql`에서 해당 컬럼과 unique 인덱스를 제거(아직 어디에도 배포되지 않은 마이그레이션이라 V3 없이 V2를 직접 수정), `LibraryBookRepositoryTest`에서 관련 테스트 1개 제거(9→8개)했다. `docs/api/decisions/0007-drop-title-author-duplicate-check.md`(ADR-0007) 신설, `.harness/DOMAIN.md`("중복" 절 단순화 + 미결정 도메인 항목 제거, 이제 ISBN 유일성 판정만 남음), `docs/api/README.md`, `.harness/STATE.md`, `ARCHITECTURE.md`에 반영. `./gradlew check` 재통과 확인.

사용자가 추가로 "UpdateLibraryBookRequest는 아직 없는데 만들 예정이라는거지? required는 여기서 할예정이고?"라고 물었다 — Java DTO(`UpdateLibraryBookRequest` record 등)는 아직 코드에 없고 "Library CRUD API" 티켓에서 컨트롤러와 함께 만들 예정이며, 거기서 Bean Validation으로 openapi.yaml의 `required`를 강제하게 될 것이라고 답했다(지금은 `LibraryBook.updateMetadata`의 수동 null 체크가 유일한 강제 지점).

커밋 여부는 아직 사용자에게 확인받지 않았다.

다음 세션 시작 시: 이번 LibraryBook 도메인/영속성 구현(포트/어댑터 분리, Adapter 리네이밍, Lombok 적용, updateLibraryBook 전체 필드화(ADR-0006), 제목·저자 중복판정 제거(ADR-0007) 포함)이 커밋됐는지 확인하고, 안 됐다면 커밋 의사를 먼저 확인한다. 그다음 `.harness/PLAN.md`의 다음 섹션("Library CRUD API")을 계획할지 사용자에게 확인한다 — 그 티켓에서 `UpdateLibraryBookRequest` 등 Java DTO와 Bean Validation을 실제로 만든다.

## 2026-08-21: 책장(Shelf) 관리 API 계약 설계 (ADR-0008)

사용자가 책장 관리 기능표(책장 생성/책장에 책 넣기(default 책장 존재)/책장 수정(이름)/책장 삭제(내부 책은 default로 이동)/책장별 책 목록 조회/책장 목록 조회)를 제시하며 `openapi.yaml`과 관련 산출물 수정을 요청했다. 이 저장소 워크플로우(기능 변경은 구현 전 `.harness/PLAN.md` 초안 → 사용자 확인)에 따라, 계약을 바로 고치는 대신 먼저 `PLAN.md`에 설계 제안을 적었다.

가장 큰 설계 쟁점은 `shelfRank`(ADR-0004, CLIAR-31에서 이미 entity·영속성·DB unique 제약까지 구현됨)의 유일성 범위였다 — 지금은 `memberId` 전역인데, 책장이 여러 개가 되면 "책장별 책 목록 조회"가 그 책장 안의 순서를 보여줘야 자연스러워 `shelfId`별로 좁히는 걸 제안했다. `AskUserQuestion`으로 3가지를 확인받았고 전부 권장안대로 확정됐다: (1) `shelfRank` 범위를 책장별로 축소, (2) 기본 책장은 계정 생성 이벤트 없이 필요 시점에 서버가 자동 생성(get-or-create) — 이 서비스가 회원가입 이벤트를 소유하지 않으므로, (3) 기본 책장은 삭제만 금지하고 이름 변경은 허용.

구현: `docs/api/openapi.yaml`을 0.5.0→0.6.0으로 올리고 `Shelf` 태그, 신규 endpoint 5개(`POST/GET /api/v1/library/shelves`, `PATCH/DELETE /api/v1/library/shelves/{shelfId}`, `GET /api/v1/library/shelves/{shelfId}/books`) + 책 이동 endpoint(`PATCH /api/v1/library/books/{bookId}/shelf`, `moveLibraryBookToShelf`)를 추가했다. 관련 스키마(`CreateShelfRequest/Response`, `ShelfSummary`, `ShelfListResponse`, `UpdateShelfRequest/Response`, `MoveLibraryBookToShelfRequest/Response`)와 오류 응답(`InvalidShelfTarget`, `LibraryBookMoveAccessDenied/NotFound`, `InvalidShelfData`, `ShelfAccessDenied`, `ShelfNotFound`, `DefaultShelfCannotBeDeleted`) 신설. 기존 `CreateLibraryBookRequest`에 선택적 `shelfId`(생략 시 기본 책장), `CreateLibraryBookResponse`/`LibraryBookSummary`/`LibraryBookDetailResponse`에 `shelfId` 노출, `getLibraryBooks`에 선택적 `shelfId` 필터(하위 호환을 위해 생략 시 전체 책장 합산 유지) 추가. `reorderLibraryBook`/`InvalidReorderTarget`의 설명을 "서재"에서 "책장"(범위) 기준으로 갱신. Python 스크립트(`$ref` 무결성·중복 `operationId`·미사용 schema/response/parameter 검사)로 계약을 검증했다(22개 operationId, 이슈 없음) — 스크립트는 세션 스크래치패드에만 있고 저장소에는 커밋하지 않았다.

`.harness/DOMAIN.md`에 `Shelf` aggregate 절 신설, `LibraryBook`/`shelfRank` 절을 책장 범위로 갱신, 최상단 범위 서술에 "책장(Shelf) 관리" 추가. `docs/api/decisions/0008-shelf-management.md`(ADR-0008) 신설, `docs/api/README.md` ADR 목록 갱신. `.harness/STATE.md`에 이번 계약 설계를 완료 단계로 기록(코드 구현은 전혀 안 됐다는 점을 명시). `.harness/PLAN.md`는 "책장(Shelf) 관리 API" 섹션을 설계 제안 서술에서 구현 체크리스트로 정리했고, 기존 "Library CRUD API" 섹션(`createLibraryBook`/`getLibraryBooks`/`reorderLibraryBook` 항목)에 `shelfId`/`shelfRank` 책장 범위 재조정을 먼저 반영해야 한다는 주의 문구를 추가했다.

**아직 하지 않은 것(중요):** `Shelf` entity/영속성 구현, 이미 구현된 `LibraryBook`(CLIAR-31)에 `shelf_id` 컬럼 추가와 `V2__create_library_book.sql`의 unique 제약을 `member_id+shelf_rank`→`shelf_id+shelf_rank`로 바꾸는 작업, `LibraryBookRepository`/`LibraryBookJpaRepository`/`LibraryBookRepositoryJpaAdapter`의 책장 범위 재조정, controller/service, 계약 테스트 — 전부 `.harness/PLAN.md`에 체크리스트로만 있다. 작업은 이미 존재하던 `CLIAR-47-책장-기능-기획-추가` 브랜치에서 진행했다(별도 브랜치 생성 불필요). 이번 세션 변경사항(openapi.yaml, DOMAIN.md, ADR-0008, README, STATE, PLAN)의 커밋 여부는 아직 사용자에게 확인받지 않았다.

다음 세션 시작 시: 이 세션의 계약 변경(`CLIAR-47-책장-기능-기획-추가` 브랜치)이 커밋됐는지 확인하고, 안 됐다면 커밋 의사를 사용자에게 먼저 확인한다. 그다음 책장 기능 구현을 실제 구현 티켓으로 시작할지, 아니면 `.harness/PLAN.md`의 다른 섹션("Library CRUD API" 등)을 먼저 할지 사용자에게 확인한다.

## 2026-08-21 (계속): Library CRUD API + 책장(Shelf) 관리 구현 (CLIAR-32)

이미 체크아웃되어 있던 `CLIAR-32-Library-CRUD-API` 브랜치(원격과 동기화, working tree clean)에서 "Library CRUD API 진행해" 요청을 받았다. `.harness/PLAN.md`를 보니 이 섹션 구현 전에 ADR-0008(책장)의 `shelf_id` 컬럼·unique 제약 재조정이 선행돼야 한다는 주의 문구가 있어, `AskUserQuestion`으로 순서를 확인했고 "CLIAR-32 하나에 책장 구현까지 포함"으로 확정받았다(권장안). 이어서 설계 결정 몇 가지(author 필터 정확히 일치 vs 부분검색, `createLibraryBook`에 잘못된 `shelfId`를 보낸 경우 신규 오류코드 추가 vs 기존 400 재사용)를 추가로 확인받고 — 둘 다 권장안(정확히 일치, 기존 400 재사용) — `PLAN.md`에 통합 설계를 적은 뒤 구현을 시작했다.

구현하며 설계 단계에서 발견한 것: `getLibraryBook`/`updateLibraryBook`/`deleteLibraryBook`/`reorderLibraryBook`/`moveLibraryBookToShelf`/`updateReadingProgress`·`updateShelf`/`deleteShelf`/`getShelfBooks` 전부 계약상 403과 404를 별도 응답으로 구분하는데, CLIAR-31에서 만든 `LibraryBookRepository.findOwnedBook`(소유자로 스코프한 단일 조회)로는 이 둘을 구분할 수 없었다 — "존재하지만 남의 것"과 "아예 없음"이 똑같이 빈 결과로 나오기 때문. `findOwnedBook`을 스코프 없는 `findById`로 교체하고 서비스 계층에서 `조회 → 없으면 404 → 소유자 다르면 403` 순서로 명시적으로 검사하는 패턴으로 바꿨다(`Shelf`도 동일). 또한 `InvalidReorderTargetException`이 CLIAR-29(공통 계약 인프라, concrete 예외 13종 생성 시점)엔 `reorderLibraryBook`(ADR-0004) endpoint 자체가 아직 없어서 빠져 있었던 것을 발견해 이번에 추가했다 — 신규 concrete 예외는 이걸 포함해 총 6종(`InvalidReorderTargetException`, `InvalidShelfTargetException`, `InvalidShelfDataException`, `ShelfAccessDeniedException`, `ShelfNotFoundException`, `DefaultShelfCannotBeDeletedException`).

`build.gradle`에 `spring-boot-starter-validation`을 추가했다가, Bean Validation의 `@Valid`/`MethodArgumentNotValidException`이 endpoint마다 다른 stable error code(예: `INVALID_BOOK_DATA` vs `INVALID_SHELF_DATA`)를 요구하는 이 저장소의 `GlobalExceptionHandler` 설계와 맞지 않아(전역 핸들러가 어느 endpoint에서 왔는지 모름) 되돌렸다 — 대신 도메인 계층(`LibraryBook`/`Shelf`)의 `IllegalArgumentException`을 서비스가 상황별 concrete 예외로 번역하는 기존 패턴을 그대로 확장했고, 컨트롤러는 도메인이 검증할 수 없는 지점(요청 DTO의 boxed `Integer` totalPages/currentPage가 JSON에서 생략돼 primitive로 언박싱할 수 없는 경우)만 직접 null 체크한다.

구현 범위: `Shelf` aggregate(`create`/`rename`)+포트/어댑터(`ShelfRepository`/`ShelfJpaRepository`/`ShelfRepositoryJpaAdapter`, `LibraryBookRepository`와 동일 패턴)+`ShelfService`(`getOrCreateDefaultShelf` 동시성 처리 포함), `LibraryBook.shelfId`/`changeShelfId` 추가, `V3__add_shelf_and_rescope_library_book.sql`(`shelf` 테이블+기본 책장 부분 unique 인덱스, `library_book.shelf_id`+FK 추가, 데이터 백필, `shelfRank` unique 범위를 `member_id`→`shelf_id`로 교체 — `V2`는 이미 `develop`에 병합돼 직접 수정하지 않음), `LibraryBookRepository`(포트) 개편(`findById`, `findPage`/`findPageOrderByProgress`로 페이징·정렬·필터, `countByShelfId`), `LibraryBookService`(전체 유스케이스 + `reorderLibraryBook`의 rebalance 오케스트레이션), `LibraryBookController`/`ShelfController`(`com.chc.dpgb.library.web`) + DTO record 20종(`com.chc.dpgb.library.web.dto`).

`getLibraryBooks`의 정렬은 Querydsl 없이 Spring Data JPA로 처리했다 — `SHELF_ORDER`/`TITLE`/`AUTHOR`/`CREATED_AT`은 `Pageable`의 `Sort`를 `@Query`에 자동 결합시키고, 저장 컬럼이 아닌 `PROGRESS`(currentPage/totalPages 계산값)만 계산식 `ORDER BY`를 쓰는 전용 asc/desc 쿼리 2개로 분리했다. `LibraryBookRepositoryJpaAdapter`/`ShelfRepositoryJpaAdapter`의 `save()`를 `saveAndFlush()`로 바꿔, unique 제약 위반(isbn 중복, 기본 책장 동시 생성)이 서비스 메서드 호출 시점에 동기적으로 드러나 `DataIntegrityViolationException`을 그 자리에서 잡아 번역할 수 있게 했다.

기존 `LibraryBookTest`(register 시그니처 변경 대응)와 `LibraryBookRepositoryTest`(shelf 범위로 전면 재작성, 실제 `Shelf` row를 먼저 저장하도록 FK 제약에 맞춰 수정)를 갱신하고, `ShelfRepositoryTest`(신규, 기본 책장 unique 제약 검증), `ShelfServiceTest`/`LibraryBookServiceTest`(Mockito, get-or-create 동시성·소유권 404/403·reorder 검증 및 rebalance·중복 409 등), `LibraryBookControllerTest`/`ShelfControllerTest`(`@WebMvcTest`, `SecurityConfigTest` 패턴 재사용 — `jwt()`에 `subject("member-1")`를 반드시 지정해야 서비스 목의 `eq("member-1")` 매처가 맞물린다는 점에 주의)를 새로 작성했다. `./gradlew check`(Docker Desktop 기동, 실제 PostgreSQL Testcontainers)로 단위+통합 테스트 전체 통과를 확인했다.

`.harness/PLAN.md`에서 "Library CRUD API + 책장(Shelf) 관리" 섹션과 "Reading Progress API" 섹션(같이 구현됨)을 제거, `.harness/STATE.md`에 단계 요약 반영, `.harness/ARCHITECTURE.md`(패키지 구조·테스트 구조) 갱신. 컨트롤러 자체 검증(400) 외의 403/404/409 등 서비스 예외 경로는 MockMvc가 아니라 서비스 단위 테스트로만 검증했다는 점을 "계약 테스트 전수화" 섹션에 남겨뒀다.

커밋 여부는 아직 사용자에게 확인받지 않았다 — 다음 행동 전에 물어볼 것.

다음 세션 시작 시: 이번 CLIAR-32 구현(Shelf 전체, LibraryBook 재조정, controller/service, DTO, 신규 예외 6종, `V3` 마이그레이션, 테스트)이 커밋됐는지 git log로 확인하고, 안 됐다면 커밋 의사를 사용자에게 먼저 확인한다. 그다음 `.harness/PLAN.md`의 다음 섹션("Book Discovery API" 외부 연동, "Scrap CRUD API", "Librarian API" 중 하나) 또는 "계약 테스트 전수화" 보강을 진행할지 사용자에게 확인한다.

## 2026-08-21 (계속): Book Discovery API 구현 — 스텁 대신 실제 알라딘 연동 (CLIAR-34)

이미 체크아웃되어 있던 `CLIAR-34-Book-Discovery-API-외부-연동` 브랜치에서 시작했다. PLAN.md에는 "자격 증명이 없으니 어댑터+스텁"으로 계획되어 있었는데, 구현 도중 사용자가 IDE 선택으로 `.env` 파일의 `ALADIN_API_TTB_KEY`(실제 알라딘 TTBKey)를 알려줘서 스텁 없이 바로 실제 연동을 구현하는 것으로 방향을 바꿨다. (직전에 스텁 vs 실연동을 묻는 `AskUserQuestion`을 한 번 거부당했었는데, 이 시점에 실제 키를 알려준 것으로 보아 굳이 다시 물을 필요 없이 진행하면 되는 상황이었다.)

`.env`는 `.gitignore`에 이미 등록되어 있어 안전하게 값을 확인했다(`git ls-files .env` 결과 없음). 이 앱이 `.env`를 자동으로 읽는 메커니즘이 없다는 것도 확인했다 — 기존 `AUTH_ISSUER_URI`/`AUTH_APP_CLIENT_ID`와 동일하게 순수 env var 주입 방식을 따르기로 하고, `application.yaml`에 `book-service.aladin.ttb-key: ${ALADIN_API_TTB_KEY}`(기본값 없음)를 추가했다.

구현 전에 실제 알라딘 API를 라이브로 여러 번 호출해(curl, TTBKey 사용) 응답 형태를 직접 확인했다 — 문서(DECISIONS.md 2026-08-20)에는 없던 사실 세 가지를 새로 발견했다: (1) 알라딘은 오류도 HTTP 200으로 응답하고 바디에 `{"errorCode":..,"errorMessage":..}`를 담는다 — HTTP status 기반 예외 처리로는 절대 못 잡는다. (2) `OptResult=itemPage`를 붙여도 `subInfo.itemPage`는 테스트한 모든 검색에서 빈 객체였다 — "totalPages 대부분 없음" 결정이 실측으로 재확인됐다. (3) `isbn`(10자리) 필드가 "K"로 시작하는 알라딘 내부 코드인 경우가 실제로 있고, `isbn13`(13자리)은 항상 정상적인 숫자였다.

구현: `com.chc.dpgb.discovery` 패키지 — `ExternalBook`(record, 포트 반환 타입), `BookDiscoveryClient`(포트), `BookDiscoveryService`(title/author 둘 다 없으면 400). 실제 알라딘 연동은 `com.chc.dpgb.discovery.aladin` 하위에 전부 package-private로 격리 — `AladinBookDiscoveryClient`(`RestClient` 사용, title만/author만/둘다 있음에 따라 QueryType을 Title/Author/Keyword로 분기), `AladinSearchResponse`/`AladinItem`/`AladinSubInfo`(`@JsonIgnoreProperties(ignoreUnknown = true)`), `AuthorNameNormalizer`(역할 라벨 제거, 이미 DOMAIN.md에 서술된 규칙 그대로 구현). `com.chc.dpgb.discovery.web`에 `BookDiscoveryController`(`GET /api/v1/books/search`) + `BookSearchResponse` DTO.

구현 중 겪은 문제 둘: (1) `RestClient.Builder`가 `spring-boot-starter-webmvc`만으로 자동구성되지 않아(Boot 4.1 세분화 모듈) `./gradlew integrationTest`가 `NoSuchBeanDefinitionException`으로 실패 — `spring-boot-starter-restclient`를 `build.gradle`에 추가해 해결. (2) `AladinBookDiscoveryClient`(TTBKey를 `@Value`로 읽는 빈)에 `@Lazy`만 붙이고 `BookDiscoveryService` 생성자 주입 지점에는 안 붙였더니, `ALADIN_API_TTB_KEY`가 없는 이 세션의 실행 셸에서 `./gradlew integrationTest`가 즉시 실패 — CLIAR-28의 `JwtDecoder` 때와 똑같은 원인이라 같은 해법(양쪽 `@Lazy`)을 적용해 해결. 둘 다 `.harness/DECISIONS.md`에 기록.

테스트: `AuthorNameNormalizerTest`(순수 함수), `AladinBookDiscoveryClientTest`(`MockRestServiceServer`에 라이브 호출로 캡처한 실제 응답 JSON을 fixture로 사용 — 네트워크 미사용, QueryType 분기·author 정규화·isbn13 우선순위·errorCode→502 변환 검증), `BookDiscoveryServiceTest`(Mockito), `BookDiscoveryControllerTest`(`@WebMvcTest`). `ALADIN_API_TTB_KEY`를 설정하지 않은 채로 `./gradlew check`(Docker Desktop 기동, 실제 PostgreSQL)를 실행해 앱이 정상 기동하는 것도 재확인했다.

`.harness/PLAN.md`에서 "Book Discovery API" 섹션 제거, `.harness/STATE.md`에 단계 요약 반영, `.harness/ARCHITECTURE.md`(discovery 패키지 구조, `spring-boot-starter-restclient`, 비밀값/`.env`/`@Lazy` 컨벤션을 다루는 새 절), `.harness/DECISIONS.md`(라이브 호출로 확인한 사실들과 두 가지 문제 해결 기록), `.harness/BACKLOG.md`(dotenv 도입 검토, Aladin 호출량 제한 대비 없음)에 반영했다.

커밋 여부는 아직 사용자에게 확인받지 않았다.

다음 세션 시작 시: 이번 CLIAR-34 구현이 커밋됐는지 확인하고, 안 됐다면 커밋 의사를 먼저 확인한다. `.env` 파일 자체는 계속 untracked 상태로 남아있어야 하므로 커밋 시 실수로 포함되지 않는지 다시 한번 확인할 것. 그다음 `.harness/PLAN.md`의 남은 섹션("Scrap CRUD API", "Librarian API", "계약 테스트 전수화") 중 무엇을 진행할지 사용자에게 확인한다.

## 2026-08-22: 계약 테스트 전수화 (CLIAR-35)

이미 체크아웃되어 있던 `CLIAR-35-계약-테스트-전수화` 브랜치(working tree clean)에서 "계약 테스트 전수화 진행해" 요청을 받았다. `.harness/PLAN.md`에 남아있던 미결 판단("MockMvc 계약 테스트로도 전수화할지는 이 섹션에서 판단")을 먼저 계획 초안으로 정리해 제시했다 — `docs/api/openapi.yaml`의 22개 operationId를 파이썬 스크립트로 파싱해 각 operationId의 문서화된 응답 코드를 전부 뽑고, 기존 `*ControllerTest`(`@WebMvcTest`)가 성공 경로 + 컨트롤러 자체 400(필수 필드 누락 등)만 다루고 서비스가 던지는 403/404/409/502는 `*ServiceTest`(Mockito) 수준에서만 검증하고 있다는 갭을 확인했다.

401(인증 실패) 테스트를 operationId마다 반복할지, 컨트롤러당 대표 1개만 둘지, 아예 추가하지 않을지를 `AskUserQuestion`으로 확인받았다 — "컨트롤러당 대표 1개(권장)"로 확정. 모든 endpoint가 동일한 `SecurityFilterChain`(`anyRequest().authenticated()`)을 타므로 이미 `SecurityConfigTest`가 필터 체인 동작을 한 번 검증하고, `LibrarianControllerTest`가 이미 이 패턴(대표 1개)으로 401 테스트를 갖고 있었다는 점이 이 선택을 뒷받침했다.

구현: `LibraryBookControllerTest`(29개, +15)/`ShelfControllerTest`(15개, +9)/`ScrapControllerTest`(18개, +12)/`BookDiscoveryControllerTest`(5개, +2)에 서비스가 던지는 concrete 예외별 MockMvc 테스트를 추가했다 — 각 컨트롤러의 기존 `@MockitoBean` 서비스에 `thenThrow`/`doThrow`로 해당 예외를 주입하고 status + `jsonPath("$.code")`를 검증하는, `LibrarianControllerTest`의 기존 404 테스트와 동일한 패턴을 그대로 재사용했다. 구현 전에 각 서비스 소스(`LibraryBookService`/`ShelfService`/`ScrapService`)를 직접 읽어 실제로 어떤 예외가 어느 시점에 던져지는지 확인했다 — 예를 들어 `getShelfBooks`는 `shelfService.getOwnedShelf`가 403/404를 던지지 `LibraryBookService`가 아니라는 점, `ScrapController`는 책 스코프(`createScrap`/`getScraps`)와 스크랩 스코프(`getScrap`/`updateScrap`/`deleteScrap`)가 서로 다른 예외 쌍(`LibraryBook*` vs `Scrap*`)을 던진다는 점을 코드로 재확인한 뒤 테스트를 작성했다. 500(`INTERNAL_ERROR`)은 계약에 없는 fallback이라 대상에서 제외했다(기존 `GlobalExceptionHandlerTest`의 일반 검증으로 충분).

`./gradlew compileTestJava`와 `./gradlew test`로 전체 통과를 확인했다(신규 테스트 포함, 기존 테스트 회귀 없음). `.harness/PLAN.md`에서 "계약 테스트 전수화" 섹션을 제거하고 `.harness/STATE.md`에 단계 요약(테스트 개수, 401 방침, 사용된 예외 목록)을 반영했다.

이번 세션은 통합 테스트(`integrationTest`/`check`)는 실행하지 않았다 — 이 티켓이 순수 MockMvc 단위 테스트 추가라 Docker/PostgreSQL Testcontainers가 필요 없었기 때문(레포 정책상 기본 검증은 `./gradlew test`만). 커밋 여부는 아직 사용자에게 확인받지 않았다 — 다음 행동 전에 물어볼 것.

다음 세션 시작 시: 이번 CLIAR-35 구현(5개 컨트롤러 테스트 전수화, 총 46개 신규 테스트)이 커밋됐는지 git log로 확인하고, 안 됐다면 커밋 의사를 먼저 확인한다. `.harness/PLAN.md`가 현재 비어 있어(완료 항목 없음) 다음에 무엇을 할지는 전적으로 사용자에게 확인해야 한다 — `.harness/BACKLOG.md`(실제 Cognito 연동 재검증, testcontainers 버전 고정 재검토, dotenv 도입, Aladin 호출량 제한 대비, 공유 DB 스키마 구조 재검토 등)를 참고 후보로 제시할 수 있다.

## 2026-08-22 (계속): Swagger(API 문서 뷰어) 도입

CLIAR-35 커밋(`74bb92a`) 이후 같은 세션에서 "swagger API 작성해" 요청을 받았다. `.harness/PLAN.md`에 계획 초안을 먼저 적었다 — 이 저장소는 `docs/api/openapi.yaml`을 wire 계약의 유일한 소스로 못박아뒀는데(`CLAUDE.md` 단일 소유권 표), 흔한 "Swagger 붙이기" 방식인 `springdoc-openapi`는 컨트롤러 애노테이션에서 스펙을 코드로 자동 생성해 소스가 둘로 나뉘는 문제가 있었다. `AskUserQuestion`으로 확인한 결과 A안(정적 뷰어 — `docs/api/openapi.yaml`을 그대로 두고 `org.webjars:swagger-ui`로 렌더링만)로 확정됐다. springdoc B안은 최신 버전(2.8.6)이 이 저장소의 Spring Boot 4.1.0/Jackson 3 조합에서 검증되지 않았다는 점도 함께 확인했었다(Maven Central 조회 결과 기반). 문서 페이지의 인증 요구 여부도 별도로 확인받아 "문서 경로만 permitAll"로 확정했다.

구현: `build.gradle`에 `org.webjars:swagger-ui:5.25.3` 추가 + `processResources`에 `docs/api/openapi.yaml`→`static/openapi.yaml` 복사를 연결(빌드 시 자동 동기화). `src/main/resources/static/docs/index.html`이 webjar 자산으로 Swagger UI를 띄우고 `/openapi.yaml`을 로드한다. `SecurityConfig`의 `authorizeHttpRequests`에 `/docs/**`/`/webjars/**`/`/openapi.yaml` `permitAll()`을 `anyRequest().authenticated()`보다 먼저 추가했다. `SecurityConfigTest`에 문서 경로가 토큰 없이 200을 반환하는 테스트를 추가.

구현 중간에 브랜치 정책을 놓친 것을 스스로 발견했다 — `CLAUDE.md`는 "현재 브랜치가 진행 중인 작업과 다른 티켓이면 새 브랜치를 만든다"고 못박아뒀는데, CLIAR-35 브랜치에서 그대로 파일을 만들기 시작한 뒤에야 이를 인지했다. `AskUserQuestion`으로 확인한 결과 사용자가 "현재 CLIAR-35 브랜치에 그대로 진행"을 선택해 별도 브랜치 없이 계속했다(티켓 번호가 없는 작업이라 별도 티켓 브랜치를 만들 근거도 약했음).

검증: `./gradlew compileJava compileTestJava`, `./gradlew test`(신규 `SecurityConfigTest` 케이스 포함) 통과. `docker compose up -d`로 로컬 PostgreSQL을 띄우고 `./gradlew bootRun --args='--spring.profiles.active=local'`로 실제 앱을 구동해 `curl`로 `/docs/index.html`/`/openapi.yaml`/`/webjars/swagger-ui/5.25.3/*` 전부 200, `/api/v1/librarians`(토큰 없음)는 여전히 401임을 직접 확인했다. 확인 후 프로세스와 `docker compose down`으로 정리했다.

`.harness/PLAN.md`에서 "Swagger(API 문서 뷰어) 도입" 섹션 제거, `.harness/STATE.md`에 단계 요약 반영, `.harness/ARCHITECTURE.md`(기술 스택에 webjars 추가, 저장소 구조에 `static/docs/index.html` 추가, "API 문서" 절에 런타임 뷰어 경로 추가) 갱신.

커밋 여부는 아직 사용자에게 확인받지 않았다 — 다음 행동 전에 물어볼 것.

다음 세션 시작 시: 이번 Swagger 문서 뷰어 작업(build.gradle, SecurityConfig, static/docs/index.html, SecurityConfigTest)이 커밋됐는지 git log로 확인하고, 안 됐다면 커밋 의사를 먼저 확인한다. 이 작업은 Jira 티켓 없이 CLIAR-35 브랜치 위에서 진행됐다는 점을 커밋 메시지/PR 설명에 반영할 것.

## 2026-08-22 (계속): 대표 사서 선택을 Member 서비스로 이관 (ADR-0009)

사용자가 자신이 작업 중인 Member 서비스 쪽 ERD 초안(DBML)을 붙여넣고 리뷰를 요청했다 — `USER.representative_librarian_id` 컬럼과 Book Service가 이미 CLIAR-46에서 구현한 `member_librarian_selection` 테이블이 같은 사실(회원의 대표 사서)을 중복 저장하는 문제, `USER`/`SOCIAL_ACCOUNT`만 스크리밍 케이스인 네이밍 불일치, `USER`가 PostgreSQL 예약어인 점(이 프로젝트가 지금까지 "member/회원" 용어를 써온 것과도 불일치), status/provider/type이 자유 varchar+note로만 표현된 점, 소프트 삭제 전파 정책 미정 등을 지적했다.

사용자가 "selectMyLibrarian 부분은 그럼 user측(Member 서비스)에 가있는게 맞겠네?"라고 되물어, `getMyLibrarian`도 같은 데이터를 다루므로 함께 옮겨야 한다는 점(선택 API만 없애면 조회 API가 참조할 데이터가 사라짐)을 짚어주고 동의했다. `getLibrarians`(사서 마스터 카탈로그)는 `selectMyLibrarian`의 `librarianId` 유효성 검사 참조 지점이라 범위 밖으로 남겼다 — 카탈로그를 Member 서비스가 어떻게 참조할지는 이 ADR 밖으로 미뤘다.

`.harness/PLAN.md`에 제거 초안을 먼저 적어 제시했고, 이어서 사용자가 요청한 DBML 수정(테이블명 snake_case, `user`→`member`, enum 적용) 중 "소프트 삭제 정책"은 대화로 정하자고 명시적으로 요청해 `AskUserQuestion`으로 두 가지를 확인받았다: (1) 탈퇴 시 email/nickname 익명화(재가입 허용, 권장안), (2) 탈퇴 회원의 Book Service 데이터(서재/책장/스크랩)는 지금 손대지 않음(이벤트 인프라 없음, 권장안) — `BACKLOG.md`로 이연.

구현: `docs/api/openapi.yaml`(v0.7.0)에서 `getMyLibrarian`/`selectMyLibrarian` endpoint, `MyLibrarianResponse`/`SelectLibrarianRequest` 스키마, `LibrarianNotSelected`/`LibrarianNotFound` 응답 제거(파이썬 스크립트로 `$ref` 무결성·미사용 스키마/응답·중복 operationId 재검증, 22→20개). `docs/api/decisions/0009-remove-representative-librarian-selection.md`(ADR-0009) 신설, `docs/api/README.md` 갱신. `com.chc.dpgb.librarian`에서 `MemberLibrarianSelection`(domain), `MemberLibrarianSelectionRepository`+JPA 어댑터(application/infrastructure), `LibrarianService.getMyLibrarian`/`selectMyLibrarian`, `LibrarianController`의 두 메서드, `MyLibrarianResponse`/`SelectLibrarianRequest`(dto), `LibrarianNotSelectedException`/`LibrarianNotFoundException`(다른 곳에서 미사용 확인 후)을 삭제하고 `LibrarianService`/`LibrarianController`를 `getLibrarians`만 남도록 재작성했다. `MemberLibrarianSelectionTest`(도메인)/`MemberLibrarianSelectionRepositoryTest`(통합) 삭제, `LibrarianServiceTest`/`LibrarianControllerTest`를 카탈로그 조회 + 401 케이스만 남도록 축소. `V6__drop_member_librarian_selection.sql` 신설로 테이블 DROP(`V5`는 이미 `develop`에 병합돼 직접 수정하지 않음, `librarian` 마스터·시드 데이터는 유지).

`docs/db/erd.dbml`에서 `member_librarian_selection` 테이블 제거, `librarian.type`을 DBML `Enum`(`CAT`/`BIRD`)으로 바꿨다. `.harness/DOMAIN.md`("Librarian / 대표 사서" 절을 "Librarian / 동물 사서 카탈로그"로, 대표 사서 선택 규칙 전체를 "Member 서비스 소유, 범위 밖"으로 정리), `.harness/ARCHITECTURE.md`(librarian 패키지 구조·V6 마이그레이션·테스트 목록), `.harness/BACKLOG.md`(탈퇴 회원의 Book Service 데이터 처리 이연) 반영. `./gradlew compileJava compileTestJava compileIntegrationTestJava`, `./gradlew check`(Docker Desktop 기동, 실제 PostgreSQL — `V6` DROP TABLE 포함) 전체 통과 확인. `.harness/PLAN.md`에서 해당 섹션 제거, `.harness/STATE.md`에 단계 요약 반영.

마지막으로, 사용자가 애초에 보내준 Member 서비스 DBML을 이번 세션에서 합의된 기준(snake_case, `user`→`member`, enum, `representative_librarian_id` 컬럼 제거·`member_librarian_selection`만 유지, 탈퇴 시 익명화 semantics, cross-service `ref` 미사용)으로 다시 작성해 마크다운으로 보여줬다 — Book Service 저장소에는 저장하지 않고 채팅 응답으로만 제공했다(다른 서비스의 스키마이므로 이 저장소가 소유할 문서가 아님).

커밋 여부는 아직 사용자에게 확인받지 않았다 — 다음 행동 전에 물어볼 것.

다음 세션 시작 시: 이번 ADR-0009 구현(Librarian 축소, V6 마이그레이션, openapi.yaml/erd.dbml/DOMAIN.md 갱신)이 커밋됐는지 git log로 확인하고, 안 됐다면 커밋 의사를 먼저 확인한다. Member 서비스 쪽 DBML(대화로 다시 작성해 보여준 버전)은 이 저장소에 없으니, 사용자가 그 서비스의 실제 저장소에 반영했는지는 이 세션이 알 수 없다.

## 2026-08-28: Book Discovery API를 title/author 검색에서 isbn 검색으로 전환 (CLIAR-161, ADR-0012)

사용자가 "책 등록할 때 지금 책제목과 저자명 기준으로 알라딘 api를 찾고 있는데 기능 명세서가 바뀌었다 — isbn 기준으로 찾도록 하고, isbn 중복체크해서 기존에 존재하면 이미 저장된 책에 대한 데이터를 반환하도록 만들어. isbn 속성은 unique 값으로"라고 요청했다. 세션 시작 시 이미 `CLIAR-161-Book-정보-조회-isbn-기준으로-변경` 브랜치가 체크아웃되어 있어(원격에도 존재) 별도 브랜치를 만들지 않았다.

구현 전에 기존 코드(`BookDiscoveryService`/`BookDiscoveryClient`/`AladinBookDiscoveryClient`/`LibraryBookService.createLibraryBook`)를 먼저 읽고, 문구가 모호한 지점 3가지를 `AskUserQuestion`으로 확인받았다: (1) title/author 파라미터는 완전히 제거하고 isbn 단일 필수 파라미터로 교체(권장안), (2) isbn 중복 확인은 등록(`createLibraryBook`) 시점이 아니라 검색(`/books/search`) 시점에 한다, (3) isbn 유일성 범위는 ADR-0007과 동일하게 회원별 유지(전역 아님, 권장안). 세 질문 모두 권장안대로 확정됐다. 이 설계를 `.harness/PLAN.md`에 적어 제시했고 사용자가 "이대로 진행해"로 승인했다.

구현: `docs/api/openapi.yaml`(v0.9.0) — `searchBookInfo`의 `title`/`author` 파라미터를 `isbn`(필수)으로 교체, `BookSearchResponse`를 `books: ExternalBook[]`에서 `{alreadyRegistered, libraryBook?, book?}` 단일 결과 구조로 재설계, `InvalidSearchParameter` 응답 예시 메시지 갱신. `docs/api/decisions/0012-isbn-based-book-search.md`(ADR-0012) 신설, `docs/api/README.md` ADR 목록 갱신. `.harness/DOMAIN.md`의 "외부 도서 검색 → LibraryBook 생성 경계" 절과 "중복" 절 갱신(정책 자체는 불변, 노출 시점만 검색으로 앞당김).

코드: `LibraryBookRepository.existsByIsbn(memberId, isbn): boolean`을 `findByMemberIdAndIsbn(memberId, isbn): Optional<LibraryBook>`로 확장(`LibraryBookJpaRepository`/`LibraryBookRepositoryJpaAdapter` 동반 변경) — `LibraryBookService.createLibraryBook`의 기존 409 체크와 신규 `BookDiscoveryService`가 이 메서드 하나를 공유한다. `BookDiscoveryClient.search(title, author): List<ExternalBook>`를 `lookup(isbn): Optional<ExternalBook>`로 교체하고 `AladinBookDiscoveryClient`가 알라딘 `ItemSearch.aspx` 대신 `ItemLookUp.aspx`(isbn 길이 10/13에 따라 `ItemIdType`을 `ISBN`/`ISBN13`으로 분기)를 호출하도록 재구현했다 — 기존 `AladinSearchResponse`/`AladinItem`/`AladinSubInfo` DTO와 `AuthorNameNormalizer`, "HTTP 200 + 바디 errorCode로 오류 판단" 패턴은 그대로 재사용했다. `BookDiscoveryService`가 `LibraryBookRepository`(library.application 포트)를 새로 의존하게 되어 discovery 패키지가 library 패키지를 단방향 참조하게 됐다(반대 방향 없음, 순환 없음) — `search(memberId, isbn)`이 먼저 서재를 조회해 있으면 알라딘을 호출하지 않고 신규 `BookSearchResult.alreadyRegistered(libraryBook)`를 반환하고, 없으면 `bookDiscoveryClient.lookup`으로 위임해 `found(book)`/`notFound()`를 반환한다. `BookDiscoveryController`가 다른 컨트롤러와 동일한 `@AuthenticationPrincipal Jwt` + `MemberIdResolver.resolve` 패턴으로 memberId를 얻도록 바뀌었다(이전엔 인증 principal을 쓰지 않았음). `discovery.web.dto.BookSearchResponse`가 `library.web.dto.LibraryBookDetailResponse`를 재사용해 `alreadyRegistered`일 때의 응답을 만든다.

**라이브 검증을 하지 못했다는 점이 중요하다**: CLIAR-34 때는 `.env`의 `ALADIN_API_TTB_KEY`로 실제 알라딘 `ItemSearch`를 curl로 여러 번 호출해 응답 형태를 확인했지만, 이번 세션 시점엔 `.env`에 그 키 항목은 있으나 값이 비어 있어(`ALADIN_API_TTB_KEY=` 빈 문자열) `ItemLookUp.aspx`를 실제로 호출해보지 못했다. `ItemSearch`와 동일한 `{item: [...], errorCode, errorMessage}` 응답 래퍼를 갖는다는 문서 기반 가정으로 구현·테스트(fixture)를 작성했다 — 특히 "존재하지 않는 isbn을 조회하면 빈 `item` 배열인지, 아니면 `errorCode`가 오는지"를 확인하지 못한 채 "빈 배열"이라고 가정했다(후자라면 알라딘에 없는 책 검색이 502로 잘못 처리된다). `.harness/BACKLOG.md`에 TTBKey 확보 후 최우선으로 재검증할 항목으로 남겼다.

테스트 갱신: `BookDiscoveryServiceTest`(Mockito로 재작성 — 이미 등록됨/알라딘에서 찾음/둘 다 없음 3분기 + isbn 공백·형식 오류 400), `BookDiscoveryControllerTest`(`@WebMvcTest`, JWT 인증 패턴 추가 + 3분기 + 400/401/502), `AladinBookDiscoveryClientTest`(`MockRestServiceServer`, isbn10/13 `ItemIdType` 분기 + 기존 errorCode/isbn13 우선순위 테스트를 lookup 방식으로 이식), `LibraryBookServiceTest`/`LibraryBookRepositoryTest`(mock/실제 호출을 `findByMemberIdAndIsbn`로 교체), `GlobalExceptionHandlerTest`(`InvalidSearchParameterException` 기본 메시지 변경 반영). `./gradlew compileJava compileTestJava compileIntegrationTestJava`, `./gradlew test`, `./gradlew check`(Docker Desktop을 이 세션에서 직접 기동시켜 실제 PostgreSQL Testcontainers로 검증) 전부 통과 확인. `docs/api/openapi.yaml`은 세션 스크래치패드의 파이썬 스크립트(`$ref` 무결성·중복 operationId·미사용 schema/response/parameter)로 재검증했다(operationId 26개, 이슈 없음) — 스크립트는 저장소에 커밋하지 않았다.

`.harness/PLAN.md`에서 이번 섹션 제거(미완료 계획 없음 상태로 복귀), `.harness/STATE.md`에 단계 요약 반영, `.harness/BACKLOG.md`에 라이브 검증 미완료 항목 추가·오래된 "최대 10건 검색 결과" 문구 정리.

커밋 여부는 아직 사용자에게 확인받지 않았다 — 다음 행동 전에 물어볼 것.

다음 세션 시작 시: 이번 CLIAR-161 구현(isbn 기반 검색, `BookSearchResult`, `findByMemberIdAndIsbn`, ADR-0012, 관련 테스트 전체)이 커밋됐는지 git log로 확인하고, 안 됐다면 커밋 의사를 먼저 확인한다. `ALADIN_API_TTB_KEY`가 준비되면 `ItemLookUp.aspx` 라이브 검증(특히 "찾지 못함" 케이스가 빈 배열인지 errorCode인지)을 최우선으로 하고, 필요하면 `AladinBookDiscoveryClient.lookup`/`AladinBookDiscoveryClientTest`를 실제 응답에 맞게 수정한다.

## 2026-08-28 (계속): 알라딘 API 키 확보 후 라이브 검증, errorCode 8 처리 버그 발견·수정

같은 세션에서 사용자가 `.env`의 `ALADIN_API_TTB_KEY`를 채우고 "알라딘 API 키 추가했으니 테스트해봐"라고 요청했다. 값이 비어있지 않은지만 길이로 먼저 확인한 뒤(값 자체는 노출하지 않음), curl로 실제 `ItemLookUp.aspx`를 세 가지 케이스로 호출했다: (1) isbn13(9788932917245, 어린 왕자) 조회, (2) isbn10(8932917248) 조회, (3) 존재하지 않는 isbn13(9780000000002) 조회.

(1)/(2)는 예상대로 `ItemSearch`와 동일한 `{item: [...], ...}` 래퍼로 응답했다. **(3)에서 직전 세션의 가정이 틀렸다는 게 드러났다** — 빈 `item` 배열이 아니라 `{"errorCode":8,"errorMessage":"키에 해당하는 상품이 존재하지 않습니다."}`를 반환했다. 직전 세션에 작성한 `AladinBookDiscoveryClient.lookup`은 모든 `errorCode`를 무조건 `AladinApiException`(502)으로 처리하고 있었으므로, 이 상태로는 "알라딘에 없는 책 검색"(정상적인 폴백 흐름)이 매번 502 서버 오류로 나갔을 것 — `.harness/BACKLOG.md`에 남겨뒀던 라이브 검증 필요 항목이 실제로 버그였음을 확인한 것이다.

수정: `errorCode == 8`일 때만 "찾지 못함"(`Optional.empty()`)으로 처리하고, 그 외 `errorCode`는 기존대로 502로 유지하도록 `AladinBookDiscoveryClient.lookup`을 고쳤다(상수 `ERROR_CODE_ITEM_NOT_FOUND = 8` + 왜 8만 예외인지 설명하는 주석 추가). `AladinBookDiscoveryClientTest`의 fixture를 라이브로 캡처한 실제 응답(어린 왕자 전체 필드)과 `errorCode: 8` 응답으로 교체해 "찾지 못함"과 "진짜 API 오류"(errorCode 4 등)를 구분하는 테스트로 재작성했다.

`.harness/STATE.md`(CLIAR-161 항목의 "라이브 검증 미완료" 문단을 검증 완료+발견한 문제로 교체), `.harness/ARCHITECTURE.md`(`AladinBookDiscoveryClient`/테스트 설명 갱신), `.harness/BACKLOG.md`(재검증 필요 항목 제거 — 이제 완료됨) 반영. `./gradlew compileJava compileTestJava test`, `./gradlew check`(Docker Desktop 기동, 실제 PostgreSQL) 전체 재통과 확인.

앱을 실제로 띄워 HTTP로 `/api/v1/books/search`를 호출하는 end-to-end 테스트는 하지 않았다 — 모든 endpoint가 인증을 요구하는데(`SecurityConfig`) 실제 Cognito User Pool이 없어 유효한 JWT를 발급할 방법이 없기 때문이다(기존에도 동일한 제약, `BACKLOG.md`의 "실제 Cognito 연동 재검증" 항목 참조). 알라딘 쪽은 직접 curl로, 애플리케이션 로직 쪽은 `MockRestServiceServer` 기반 슬라이스 테스트로 검증하는 것으로 대신했다.

커밋 여부는 아직 사용자에게 확인받지 않았다 — 다음 행동 전에 물어볼 것.

다음 세션 시작 시: 이번 라이브 검증·버그 수정을 포함한 CLIAR-161 전체가 커밋됐는지 git log로 확인하고, 안 됐다면 커밋 의사를 먼저 확인한다.

## 2026-08-29: 서재 책 등록의 genre/readingStatus/shelfId — 원인 규명, Swagger 예시 분리, 회귀 테스트

사용자가 "책 등록 API에서 `genre`/`readingStatus`가 입력을 안 받고 디폴트로 내보내고 있다, 등록 시 함께 입력받아 DB에 저장하면 좋겠다. `shelfId`도 없으면 기본 책장, 있으면 해당 책장으로 배치"라고 요청했다.

먼저 구현하지 않고 코드를 확인했더니 **요청한 세 가지가 이미 전부 구현되어 있었다** — `CreateLibraryBookRequest`에 세 필드가 있고, `LibraryBookController`가 그대로 넘기고, `LibraryBookService.resolveShelf`가 `shelfId==null`이면 기본 책장을 get-or-create하며, `LibraryBook.register`가 `null`일 때만 `NONE`/`PLANNED`를 채우고, `V7` 마이그레이션에 `genre_type`/`book_reading_status` PostgreSQL enum 컬럼이 있다. `docs/api/openapi.yaml`의 `CreateLibraryBookRequest` 스키마에도 세 필드가 선택 필드로 이미 명세돼 있었다. 그래서 구현 대신 이 사실을 보고하고, 어떻게 관찰했는지(실제로 값을 보냈는지/안 보냈는지/배포 문서가 오래된 건지)를 되물었다.

사용자가 "swagger 문서에 세 필드를 전부 넣은 버전과 빠진 버전 2개 예시를 넣어줘"라고 답하면서 **진짜 원인이 드러났다**: `createLibraryBook`의 요청 본문 `example`(단수)에 `genre`/`readingStatus`/`shelfId`가 아예 없어서, Swagger UI "Try it out"이 그 예시를 그대로 채워 보내면 언제나 서버 기본값으로만 등록됐던 것이다. 스키마가 아니라 예시가 문제였다. 단일 `example`을 이름 있는 `examples` 2종으로 교체했다 — `전체_입력`(세 필드 모두 지정, 드롭다운 기본 선택)과 `선택_필드_생략`(기존 예시 그대로). 기존 `searchBookInfo` 응답 예시와 같은 컨벤션(한국어 키 + `summary`/`description`)을 따랐다. 스키마·서버 동작이 바뀌지 않아 `info.version`(0.9.0)은 올리지 않았고 ADR도 만들지 않았다.

이어서 사용자가 "로컬 실행해서 swagger에 어떻게 뜨는지 확인해볼래"라고 해 실제로 띄워 검증했다: Docker Desktop 기동 → `docker compose up -d postgres` → `./gradlew bootJar` → `.env` 주입 + `SPRING_PROFILES_ACTIVE=local`로 `java -jar` 실행 → `/health` 200. `GET /openapi.yaml`이 편집한 예시 2개를 순서대로 서빙하는 것을 확인했다(`processResources`의 `docs/api/openapi.yaml` → `static/` 복사 경로 정상). 그 다음 헤드리스 Chrome + CDP(노드 v24 내장 `WebSocket`으로 직접 구현, 의존성 설치 없음)로 `/docs/index.html`을 열어 `createLibraryBook`을 실제 마우스 이벤트로 펼치고 `Examples:` 드롭다운(`<select class="examples-select-element">`)에 두 옵션이 뜨는 것과 각 예시의 렌더링을 스크린샷으로 확인했다. 참고로 Swagger UI는 `.click()`(프로그램적 클릭)으로는 오퍼레이션이 펼쳐지지 않아 `Input.dispatchMouseEvent`가 필요했고, `examples-select`의 옵션 텍스트는 예시 키가 아니라 `summary` 값으로 표시된다.

사용자가 먼저 커밋을 요청해 `develop`에 직접 커밋했다가(브랜치 정책상 티켓 브랜치가 맞지만 티켓이 없어 `AskUserQuestion`으로 물었고 사용자가 "develop 직접 커밋"을 선택), 곧바로 "커밋 되돌리고 회귀 테스트까지 해서 한꺼번에 커밋"을 요청해 `git reset --soft HEAD~1`로 되돌린 뒤 테스트를 추가했다.

회귀 테스트는 4개 계층에 넣었다(기존에는 이 pass-through를 검증하는 테스트가 하나도 없었다 — 컨트롤러 등록 테스트가 전부 `any()` 매처를 쓰고 본문에 세 필드를 넣지 않았고, 서비스/리포지토리 테스트는 등록 시 `genre`/`readingStatus`를 항상 `null`로만 호출했다):

- `LibraryBookTest`: 값을 지정하면 기본값으로 덮이지 않는다 (기존엔 "생략하면 기본값" 테스트만 있었다)
- `LibraryBookServiceTest`: 지정한 `shelfId`의 책장에 등록 / 지정한 `genre`·`readingStatus`가 결과에 반영
- `LibraryBookControllerTest`: JSON 본문 → 서비스 인자 → HTTP 응답. 세 필드에만 `eq` 매처를 걸어 컨트롤러가 값을 흘리면 스텁이 매칭되지 않아 실패하게 했고, 응답 본문(`$.shelfId`/`$.genre`/`$.readingStatus`)까지 검증해 mock `verify()` 대신 관찰 가능한 결과로 확인한다. 헬퍼 `book()`의 인라인 리플렉션을 `withBookId()`로 추출하고 `book(shelfId, genre, readingStatus)` 오버로드를 추가했다.
- `LibraryBookRepositoryTest`(통합): 지정값과 기본값 각각이 PostgreSQL enum 컬럼에 저장되고 `entityManager.clear()`(신규 `@PersistenceContext` 주입) 후 다시 읽힌다

**테스트가 실제로 실패할 수 있는지 확인했다** — `LibraryBookController`가 `genre`/`readingStatus` 자리에 `null`을 넘기도록 일부러 깨뜨리자 새 컨트롤러 테스트만 FAILED가 났고, `git checkout --`으로 원복했다. `./gradlew check`(실제 PostgreSQL Testcontainers) 전체 통과.

`.harness/STATE.md`에 단계 한 줄 요약, `.harness/BACKLOG.md`에 미결 항목(201 응답 예시를 요청 예시와 짝 맞출지) 추가. `PLAN.md`는 이번 작업이 같은 세션에서 끝나 미완료 항목이 없어 그대로 뒀다.

**다음 세션 시작 시**: 이 작업(openapi 예시 2종 + 4계층 회귀 테스트 + STATE/BACKLOG 갱신)이 한 커밋으로 `develop`에 올라가 있다. push는 하지 않았으니 필요하면 사용자에게 확인할 것. 미결 항목은 `BACKLOG.md`의 201 응답 예시 건 하나다. 로컬에 앱(`java -jar`)과 `docker compose` postgres가 떠 있는 채로 세션이 끝났을 수 있으니 필요하면 정리한다.

## 2026-08-30: prod EKS 배포 CrashLoopBackOff 원인 규명과 멀티아키 이미지 전환 (CLIAR-112)

사용자가 "prod 환경 배포를 마무리하려는데 ArgoCD에서 Degraded가 떠 있다, `backend-book` 파드가 CrashLoopBackOff(exit 255)이고 svc/ing는 정상인데 deploy 단계에서 문제가 난다"며 파드/Deployment 상태를 붙여 요청했다. 세션 시작 시 브랜치는 `develop`이었고, 계획 확정 후 사용자가 직접 `CLIAR-112-Book-Server-EKS-prod-배포` 브랜치를 만들어 옮겨둔 상태로 구현을 시작했다.

세션 앞부분은 AWS CLI 로그인 질문이었다. `~/.aws`에 `default`(장기 IAM 키)와 `mfa`(임시 세션) 두 프로필이 있고, `mfa` 프로필이 `ExpiredToken`으로 만료된 상태였다. MFA 디바이스는 두 개 등록되어 있는데 CLI에서 쓸 수 있는 건 가상 OTP(`arn:aws:iam::594532711953:mfa/otp-cli`)뿐이고 나머지 하나는 U2F 보안키(콘솔 전용)다. PowerShell용 `sts get-session-token` 재발급 절차를 안내했고 사용자가 갱신했다.

**원인 규명 과정**: prod 클러스터가 kubeconfig에 없어 `aws eks update-kubeconfig --name dpyb-prod --alias dpyb-prod`로 컨텍스트를 추가했다(사용자 머신의 `~/.kube/config` 변경). `kubectl logs --previous`로 크래시한 이전 컨테이너 로그를 보니 단 한 줄, `exec /opt/java/openjdk/bin/java: exec format error`였다 — Spring 배너도 스택트레이스도 없다는 건 JVM이 exec 시점에 실패했다는 뜻이라 애플리케이션/설정 문제가 아니다. 이어서 세 가지를 교차 확인했다: (1) `dpyb-prod` 노드는 전부 arm64(`workload=book` 전용 노드는 `t4g.medium`, 나머지 `c6g.large`), (2) ECR의 prod 이미지가 manifest list가 아닌 `manifest.v2` **단일 아키텍처**, (3) CI는 `ubuntu-latest`(amd64)에서 `docker build`를 그냥 실행. → amd64 이미지를 arm64 노드에서 돌린 것이 확정.

**dev가 멀쩡했던 이유도 밝혀 기록해뒀다**: `dpyb-dev`는 amd64(`r5a`/`c5a`)와 arm64(`c6g`)가 섞여 있고 dev overlay에는 `nodeSelector`가 없어서, dev 파드가 우연히 amd64 노드(`i-0a72686b6f55953e8`)에 착지했을 뿐이다. Karpenter consolidation·노드 교체·amd64 여유 부족 중 하나만 걸리면 dev도 같은 증상으로 죽는다. 사용자가 "dev는 혼합이라 문제없고 prod는 arm64만이라 문제냐"고 확인했을 때, "dev는 문제없는 게 아니라 우연히 안 걸린 것"이라는 점을 명확히 짚었다 — 이것이 NodePool 고정 대신 멀티아키를 택한 핵심 근거다.

`AskUserQuestion`으로 세 가지를 확정받았다: 해결 방식은 **멀티아키 이미지**(대안이던 "prod NodePool을 amd64로 고정"은 긴급 롤백 레버로만 남김), 브랜치는 사용자가 이미 만들어 옮김, 수행 범위는 **커밋까지**(push·PR·main 병합은 사용자가 직접).

**구현**: `Dockerfile`의 빌드 스테이지를 `FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build`로 고정했다 — jar이 아키텍처 중립이라 Gradle은 러너 네이티브로 한 번만 돌고, 런타임 스테이지(`eclipse-temurin:21-jre`)만 타깃 아키텍처를 따라간다. 런타임 스테이지에 `RUN`이 없으므로 QEMU 에뮬레이션 실행 자체가 없어 멀티아키 비용이 사실상 0이다. `.github/workflows/build-push-ecr.yml`은 `setup-qemu-action`/`setup-buildx-action` 추가 후 `docker build`/`docker tag`/`docker push` 스크립트를 `docker/build-push-action@v6`(`platforms: linux/amd64,linux/arm64`, `push: true`, `provenance: false`)로 교체했다. **buildx 멀티플랫폼 빌드는 로컬에 이미지가 남지 않아 `docker tag`를 쓸 수 없다** — 그래서 push할 태그 전부를 "Resolve target by branch" 스텝이 `tags` 멀티라인 출력으로 계산해 넘기도록 함께 바꿨다(prod=SHA 1개, dev=SHA + `develop-latest`).

검증: `eclipse-temurin:21-jre`에 `linux/arm64/v8` 변형이 실제로 있는지 `docker buildx imagetools inspect`로 확인(Docker 데몬 없이 레지스트리 직접 조회로 됐다). 태그 계산 셸 로직은 임시 디렉터리에서 `main`/`develop` 양쪽으로 시뮬레이션해 출력이 의도대로인지 확인. 워크플로우 YAML은 파이썬 `yaml.safe_load`로 파싱 검증. `./gradlew test` 통과(Java 코드 변경은 없다). **실제 멀티아키 빌드는 로컬에서 돌려보지 못했다** — Docker 데몬이 꺼져 있었고, 진짜 검증은 CI에서 나므로 그쪽으로 미뤘다.

문서: `.harness/ARCHITECTURE.md` 배포 절에 노드 아키텍처 현황(prod 전부 arm64/Graviton, dev 혼합)·멀티아키 CI·Dockerfile 2-스테이지 구조 반영, `.harness/DECISIONS.md` 최상단에 결정과 기각한 대안 2종 기록, `.harness/STATE.md`에 단계 한 줄 요약, `.harness/PLAN.md`를 배포·검증 잔여 단계로 재작성(긴급 롤백용 NodePool amd64 고정 YAML 스니펫 포함).

**다음 세션 시작 시**: 이 작업은 `CLIAR-112-Book-Server-EKS-prod-배포` 브랜치에 커밋만 되어 있고 **push되지 않았다**. `.harness/PLAN.md`의 잔여 체크리스트(push → develop PR → main 병합 → ECR 매니페스트가 아키텍처 2개인지 확인 → prod 파드 `2/2 Running` → 잔여 ReplicaSet 정리 → ArgoCD Healthy → `/health` 200)를 이어서 진행한다. prod ECR은 IMMUTABLE이라 기존 SHA 재push가 아닌 **새 병합 커밋 SHA**로 나가야 한다.

**중요한 미확인 사항**: prod에서 JVM이 지금까지 한 번도 기동한 적이 없으므로, 아키텍처를 고치면 그 다음 단계 문제가 처음 드러날 수 있다. prod Secret에 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`/`ALADIN_API_TTB_KEY` 4개 키가 존재하는 것까지만 확인했고 **RDS 실제 접속·Flyway 마이그레이션·Cognito 검증은 전부 미확인**이다. 파드가 뜬 직후 로그를 반드시 확인할 것 — 이번엔 죽더라도 스택트레이스가 남는다.

## 2026-08-30: prod EKS 배포 완료 — 세 겹의 원인을 순차 해소, dev DB 서비스별 분리 (CLIAR-112)

사용자가 "prod 환경 배포를 마무리하려는데 ArgoCD에서 Degraded가 떠 있다"며 시작한 세션이, 원인을 하나씩 벗겨내며 인프라 작업 여러 건으로 이어졌다. `backend-book` prod 파드는 이틀 넘게 CrashLoopBackOff였고 **원인이 세 겹으로 쌓여 있었다** — 하나를 고칠 때마다 다음 것이 드러났다.

**1겹: 아키텍처 불일치.** `kubectl logs --previous`가 `exec /opt/java/openjdk/bin/java: exec format error` 한 줄만 남긴 것이 단서였다(Spring 배너도 스택트레이스도 없음 = JVM이 exec 시점에 실패). CI가 `ubuntu-latest`에서 amd64 단일 아키텍처 이미지를 만드는데 `dpyb-prod` 노드는 전부 arm64(Graviton)였다. dev가 멀쩡했던 건 amd64/arm64 혼합 클러스터에서 파드가 우연히 amd64 노드에 착지했기 때문 — 실력이 아니라 운이었고, 재배치되면 dev도 같이 죽을 잠재 결함이었다. `Dockerfile` 빌드 스테이지를 `--platform=$BUILDPLATFORM`으로 고정하고(jar이 아키텍처 중립이라 Gradle은 1회만 돌고 QEMU 에뮬레이션 비용이 없다) CI를 `docker/build-push-action`(`platforms: linux/amd64,linux/arm64`, `provenance: false`)으로 교체했다. buildx 멀티플랫폼은 로컬 이미지가 남지 않아 `docker tag`를 못 쓰므로 push 태그 목록을 "Resolve target by branch" 스텝의 `tags` 출력으로 계산하게 함께 바꿨다. PR #19가 develop·main에 병합되어 CI 성공, ECR 이미지가 OCI index에 `linux/amd64`+`linux/arm64` 두 항목만 갖는 것을 확인했다(커밋 `598fc06`).

**2겹: 아웃바운드 부재.** 아키텍처를 고치니 JVM이 뜨면서 `FlywaySqlUnableToConnectToDbException` → `28P01`이 나왔다. 조사 중 별개 문제를 발견 — prod VPC의 private 서브넷 4개에 `0.0.0.0/0` 경로가 아예 없었다(S3 게이트웨이 + ECR 인터페이스 엔드포인트만). 외부 API를 쓰는 서비스인데 알라딘 호출이 구조적으로 불가능한 상태였고, 디버깅용 psql 이미지도 Docker Hub에서 받을 수 없었다. `public2-ap-northeast-2b`에 NAT Gateway(`nat-0c14a04ce3a253648`, EIP `52.78.78.173`)를 만들고 private 라우팅 테이블 4개 전부에 경로를 추가했다. book 노드가 2b에 있어 AZ 간 전송이 없는 배치다. 검증은 파드에서 `curl checkip.amazonaws.com`이 NAT EIP를 그대로 반환하는 것으로 했다(제 PC에서 대신 호출한 게 아님을 증명하는 유일한 방법이었고, 사용자가 이 점을 정확히 물었다). 알라딘 HTTP 200, Docker Hub pull 성공. **처음에 `logs`/`sts`/`secretsmanager` 인터페이스 엔드포인트 추가도 권했다가 철회했다** — AZ마다 ENI가 상시 과금되는데 절감 대상 트래픽이 작은 JSON 수준이라 고정비가 절감액을 웃돈다.

**3겹: DB 자체가 없음.** `admin`이 `dpyb`뿐 아니라 반드시 존재하는 `postgres` 데이터베이스로도 `28P01`로 거부되는 것을 확인해 "비밀번호가 아니라 역할이 없다"를 확정했다(prod/dev의 `DB_PASSWORD`는 해시 비교로 동일함을 확인했으므로 비밀번호 문제일 수 없었다). 사용자가 "권한 문제 아니냐"고 물었을 때 `28P01`은 인증 단계 오류이고 권한 문제면 `42501`/`3D000`이 난다는 점으로 배제했다. dev의 `admin`을 조사하니 `LOGIN`+`CREATEDB`뿐이고 `rds_superuser` 멤버십 없이 DB 소유자라는 단순한 구성이었다(소유자면 `pg_database_owner`를 통해 `public` 스키마 권한이 자동으로 따라온다). 마스터로 `admin` 역할과 `dpyb_book`/`dpyb_auth`/`dpyb_record`를 생성했다.

**중간에 Aurora 최적화도 했다.** 7일 실측이 커넥션 0, CPU 6~7%, 데이터 52MB인데 `db.r7g.large` 2대 + I/O-Optimized였다(dev는 `db.serverless`). reader 삭제, writer를 `db.serverless`(0.5~8 ACU)로 전환, 스토리지를 Standard로 바꿨다. 실측 I/O가 월 약 520만 건이라 I/O-Optimized 프리미엄이 명백한 손해였다.

**dev DB 서비스별 분리도 같은 세션에서 실행했다(팀 승인 후).** 단일 `dpyb`의 `public` 스키마 하나에 세 서비스 테이블 13개가 섞여 있었다. 서비스 경계를 넘는 외래키가 하나도 없음을 먼저 확인하고, 같은 클러스터 안에서 `dpyb_book`/`dpyb_auth`/`dpyb_record`로 나눴다(클러스터·인스턴스를 늘리지 않아 추가 비용 0). **옮기지 않고 복사**해 각 팀이 준비되면 전환하도록 했다. 두 함정 — `pg_dump -t`는 커스텀 enum 타입을 포함하지 않아 대상 DB에 6종을 먼저 만들어야 했고, `pg_dump`는 서버와 메이저 버전이 같아야 해서 `postgres:16-alpine`이 17.7 서버를 거부했다(`postgres:17-alpine`으로 교체). book은 전환·검증까지 완료했고, auth·record 팀용 런북을 Artifact로 작성해 공유했다(https://claude.ai/code/artifact/ce6455b7-be45-40bf-96c8-7bf5b6da7e40). 이 분리 덕분에 **prod는 처음부터 `dpyb_book`으로 만들어 나중에 이전할 일이 없어졌다.**

**PowerShell 인용 문제로 상당히 헤맸다는 점을 남겨둔다.** PowerShell 5.1은 네이티브 실행 파일에 인자를 넘길 때 큰따옴표를 벗겨내서, `ConvertTo-Json`으로 만든 patch가 `invalid character 's'`로 실패하고 `\"` 이스케이프도 상황에 따라 깨졌다. 그 과정에서 **에러 메시지에 새 비밀번호가 평문 출력**됐다(`BACKLOG.md`에 교체 항목 추가). 또 psql `\password`는 입력을 화면에 표시하지 않아 붙여넣기 실패를 알아챌 수 없었고, 결국 DB에 설정된 값과 Secret의 값이 어긋나 인증 실패가 한 번 더 났다 — 최종적으로 `ALTER ROLE admin PASSWORD '...'`로 DB를 Secret에 맞추는 방향으로 해결했다. **다음에 Windows에서 kubectl patch를 안내할 때는 처음부터 `--patch-file` 또는 bash 경유를 쓰는 게 낫다.**

PostgreSQL 16+ 함정 둘도 겪었다: `CREATE DATABASE ... OWNER admin`이 `must be able to SET ROLE`로 실패해 `GRANT admin TO postgres`가 선행되어야 했고, psql `\c`는 실패해도 이전 연결이 유지되어 뒤따르는 `GRANT`가 엉뚱한 DB에 걸렸다(회수 필요).

**최종 검증**: Flyway가 빈 DB에서 `V1`~`V9`를 처음부터 적용해 `now at version v9`, `Started DpgbApplication`, Deployment `2/2`, 옛 ReplicaSet 2개 0으로 축소, `/health` `{"status":"UP"}` 200, ArgoCD `backend-book-prod` **Synced/Healthy**. 임시 psql 파드는 prod·dev 모두 정리했다.

**다음 세션 시작 시**: prod는 정상 동작 중이다. 남은 것은 `.harness/PLAN.md`의 dev DB 분리 후속(auth·record 팀 전환 대기, 전원 전환 후 기존 `dpyb` 정리)과 `BACKLOG.md`의 항목들이다. 특히 **prod `admin` 비밀번호 교체**(노출됨)와 **서비스별 역할 분리**가 보안상 우선순위가 높다. 코드(`src/`)는 이번 세션에서 전혀 변경하지 않았다.

## 2026-08-30: dev Aurora 잔재 정리 — `postgres`의 `alembic_version_auth` 드롭

사용자가 "RDS에서 `test`와 `postgres` DB에 `public.alembic_version_auth` 삭제해줘"라고 요청했다. `BACKLOG.md`에 이미 올라와 있던 정리 항목이라 새 계획 절차 없이 바로 수행했다.

dev Aurora는 private 서브넷에 있어 로컬에서 직접 붙을 수 없고, 이전 세션의 `psql-dev` 파드는 `Completed` 상태였다. 그래서 `dpyb-book-dev` 네임스페이스에 `backend-book-secret`의 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`를 env로 받는 임시 파드(`psql-dev2`, `postgres:17-alpine`)를 띄워 작업하고 끝나고 삭제했다. (Git Bash에서 `kubectl exec ... -- sh /tmp/x.sh`는 경로가 Windows 경로로 변환되므로 `MSYS_NO_PATHCONV=1`이 필요했다.)

드롭 전에 대상을 먼저 확인했다. **`test` 데이터베이스에는 `alembic_version_auth`가 아예 없었다** — `flyway_schema_history`/`librarian`/`library_book`/`scrap`/`shelf` 5개뿐으로, 조사 기록(book V6 시점 스키마)과 일치한다. `postgres` 데이터베이스에만 존재했고 소유자 `admin`, 컬럼 1개(`version_num`), **0행**, 의존 객체 0이었다. 트랜잭션 안에서 행 수를 다시 확인하고 `DROP TABLE public.alembic_version_auth`를 실행한 뒤 `to_regclass`가 NULL임을 확인했다. 데이터 손실은 없다.

`BACKLOG.md`는 원래 이 건을 "auth 담당자 확인 후 드롭"으로 적어두고 있었다 — 사용자의 직접 지시로 진행했다. 빈 테이블이라 auth 쪽 실제 마이그레이션 이력(`dpyb`의 `205eb1a0a7eb`)에는 영향이 없다.

**다음 세션 시작 시**: 같은 정리 항목 중 **`test` 데이터베이스 삭제(`DROP DATABASE test;`)는 아직 남아 있다**(`PLAN.md` B절, `BACKLOG.md`). 문서만 갱신했고 커밋은 하지 않았다.

## 2026-08-30 (이어서): `test` 데이터베이스 삭제

같은 세션에서 사용자가 `test` 데이터베이스도 삭제하라고 지시했다. 중간에 `mfa` 프로필의 STS 세션 토큰이 만료돼 `kubectl`이 전부 막혔다(kubeconfig의 dev 컨텍스트가 `AWS_PROFILE=mfa`로 `aws eks get-token`을 호출하는 구조). 사용자가 재발급하려다 실패했는데, 원인은 `get-session-token` 호출 자체가 만료된 `mfa` 프로필로 나간 것이었다 — **이 호출만큼은 장기 IAM 키인 `default` 프로필로 해야 한다**(`--profile default`). 이후 정상 발급됐다.

드롭 전 상태 확인: owner `admin`, 8MB, 활성 커넥션 0, Flyway 버전 6. 행 수는 `flyway_schema_history` 6, `librarian` 2, `library_book`/`scrap`/`shelf` 0으로 조사 기록과 정확히 일치했다. `pg_dump -F c`로 백업(11KB)한 뒤 파드에서 스트리밍해 로컬로 꺼내고 md5로 대조했다 — `kubectl cp`는 Windows 드라이브 문자(`C:`)를 경로 구분자로 오인해 실패하므로 `kubectl exec -- cat > 파일`로 받았다. 그 다음 `postgres` 데이터베이스에 접속해 `DROP DATABASE test`를 실행하고 `pg_database`에서 사라진 것을 확인했다.

**백업 파일은 세션 스크래치패드에만 있다**(`dpyb-dev-test-20260830.dump`). 보존이 필요하면 사용자가 옮겨야 한다 — 다만 시드 2행 외에 실데이터가 없어 실질 가치는 낮다.

작업용 임시 파드 `psql-dev2`는 삭제했다. 참고로 `pg_database_size`를 전체 DB에 돌리면 `rdsadmin`에서 권한 거부가 나므로 목록 조회 시 제외해야 한다.

**다음 세션 시작 시**: dev Aurora 잔재 정리는 이것으로 끝났다(`BACKLOG.md`에서 항목 제거). 남은 DB 관련 미결은 **구 통합 `dpyb` 데이터베이스 정리**인데, 이건 auth·record 팀이 각자 Secret의 `DATABASE_URL`을 `dpyb_auth`/`dpyb_record`로 바꿔 전환을 마친 뒤에만 진행한다(`PLAN.md` B절). `.harness` 문서 갱신은 커밋하지 않았다.
