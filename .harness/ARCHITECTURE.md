# ARCHITECTURE (현재 상태)

이 문서는 지금 시점의 실제 기술 스택·구조·컨벤션만 담는다. 결정 이유는 `DECISIONS.md`, 진행 상황은 `STATE.md`를 본다.

## 기술 스택

- Java 21, Spring Boot 4.1.0, Gradle Wrapper 9.5.1
- Spring MVC, Spring Data JPA, Flyway(`flyway-core`, `flyway-database-postgresql` + Boot autoconfigure를 가져오는 `spring-boot-starter-flyway` — 이 starter 없이는 `FlywayAutoConfiguration`이 로드되지 않아 마이그레이션이 자동 실행되지 않는다, CLIAR-31에서 발견)
- Spring Security OAuth2 Resource Server(`spring-boot-starter-oauth2-resource-server`) — JWT 검증, 인증 서비스는 AWS Cognito User Pool
- `spring-boot-starter-restclient` — `RestClient.Builder` 자동구성(`RestClientAutoConfiguration`)을 제공. `spring-boot-starter-webmvc`에 딸려오지 않아 별도 추가(CLIAR-34, Flyway 때와 동일한 Boot 4.1 세분화 모듈 패턴). 알라딘 API 연동(`AladinBookDiscoveryClient`)에서 사용
- 기준 패키지: `com.chc.dpgb`
- DB: PostgreSQL (JDBC 드라이버 `org.postgresql:postgresql`, 스키마는 Flyway migration으로 관리, `spring.jpa.hibernate.ddl-auto: validate`)
- Testcontainers(`org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql`) — 버전은 `build.gradle` 주석 참조. `io.spring.dependency-management`가 Spring Boot BOM의 testcontainers-bom 중첩 import를 반영하지 못하고, Boot 4.1.0이 가리키는 testcontainers.version이 아직 Maven Central에 없어 실재하는 버전을 직접 고정했다. Boot 업그레이드 시 재검토.
- Lombok (compile/annotation processor) — entity getter는 `@Getter`로 생성(예: `LibraryBook`), setter는 쓰지 않고 불변식이 있는 도메인 메서드로만 상태를 바꾼다
- **soft delete 컨벤션(2026-08-25~)**: 논리 삭제를 쓰는 엔티티(`Shelf`/`LibraryBook`/`Scrap`/`Librarian`)는 `deletedAt`(Instant) 필드 + `softDelete(Instant)`/`isDeleted()` 도메인 메서드를 갖고, 클래스에 Hibernate `@SQLRestriction("deleted_at IS NULL")`(6.3+, 구 `@Where` 대체)을 붙인다 — 파생 쿼리·JPQL·`findById` 등 모든 조회에 자동 적용되어 서비스/리포지토리 코드가 조건을 반복하지 않는다. 하드 `delete()` 포트 메서드는 두지 않고, 서비스가 `entity.softDelete(Instant.now()); repository.save(entity);`로 통일한다.
- **네이티브 Postgres enum 컬럼 매핑 컨벤션**: `genre_type`/`book_reading_status`/`librarian_type`처럼 DB가 네이티브 `ENUM` 타입인 컬럼은 Java enum 필드에 `@Enumerated(EnumType.STRING) + @JdbcTypeCode(SqlTypes.NAMED_ENUM)`(Hibernate 6.2+, `org.hibernate.annotations.JdbcTypeCode`/`org.hibernate.type.SqlTypes`)로 매핑한다. 일반 `@Enumerated(STRING)` 단독으로는 JDBC 드라이버가 값을 VARCHAR로 캐스팅하려다 실패한다.
- 관측(Observability): `spring-boot-starter-opentelemetry`(Boot 4.1 공식 스택 — `micrometer-tracing-bridge-otel` + `opentelemetry-sdk`/`opentelemetry-exporter-otlp` 1.62.0) + `net.ttddyy.observation:datasource-micrometer-spring-boot:2.2.1`(JDBC 구간 span, Micrometer Observation이 커버하지 않는 유일한 구간. 2.x가 Boot 4 대응 라인). OpenTelemetry Java Agent(`-javaagent`)는 쓰지 않는다. 구조화 로그 포맷터는 `com.chc.dpgb.common.logging.JsonLogFormatter`(Boot 내장 `StructuredLogFormatter`/`JsonWriter` 구현, 외부 인코더 라이브러리 없음)
- 메트릭: `spring-boot-starter-actuator` + `io.micrometer:micrometer-registry-prometheus`(1.17.0, prometheus client 1.5.x) — infra Prometheus가 `/actuator/prometheus`를 ServiceMonitor로 스크레이핑한다. 공통 `application.yaml`은 web 노출을 비워 둬 기본으로는 엔드포인트가 0개이고, **dev overlay만** `MANAGEMENT_SERVER_PORT=8081` + `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,prometheus`로 별도 관리 포트에서 opt-in 한다(관측 절·배포 절 참조)
- `org.webjars:swagger-ui`(5.25.3) — API 문서 뷰어. springdoc 같은 애노테이션 기반 스펙 생성기가 아니라 순수 정적 HTML/JS 자산만 제공 — `docs/api/openapi.yaml`이 계약의 유일한 소스라는 원칙을 지키기 위한 선택
- 실제 버전은 `build.gradle`과 Gradle Wrapper가 최종 기준
- **Spring Boot 4.1.0 패키지 이동 주의**: Jackson은 `com.fasterxml.jackson.databind`가 아니라 `tools.jackson.databind`(Jackson 3)를 쓴다. `@WebMvcTest`는 `org.springframework.boot.webmvc.test.autoconfigure`(`spring-boot-webmvc-test` 모듈)에 있다. 예전 Boot 버전 예제 코드의 import 경로를 그대로 쓰면 컴파일 에러가 난다.

## 저장소 구조

루트 단일 Gradle 프로젝트다. `backend` 하위 모듈은 없다. `test`(단위)와 `integrationTest`(PostgreSQL Testcontainers) source set이 분리되어 있다.

```text
src/main/java/com/chc/dpgb
├─ DpgbApplication.java
├─ common
│  ├─ ErrorResponse.java              # {code, message} — API 전역 에러 응답 포맷
│  ├─ logging
│  │  └─ JsonLogFormatter.java        # prod 프로필의 stdout JSON 로그 포맷(필드명 계약의 단일 소유자)
│  ├─ observability
│  │  └─ ObservabilityConfiguration.java  # ObservationPredicate 빈 — /health·/docs·/webjars·/openapi.yaml·/actuator를 서버 span·메트릭에서 제외(CLIAR-234 + 관측-인프라-연동)
│  └─ exception
│     ├─ DomainException.java         # abstract, code() 추상 메서드 — 계층 최상위
│     ├─ BadRequestException.java     # abstract, 400
│     ├─ ForbiddenException.java      # abstract, 403
│     ├─ NotFoundException.java       # abstract, 404
│     ├─ ConflictException.java       # abstract, 409
│     ├─ BadGatewayException.java     # abstract, 502
│     ├─ GlobalExceptionHandler.java  # @RestControllerAdvice — 5개 abstract 타입 + 500 fallback(INTERNAL_ERROR)을 ErrorResponse로 매핑. 일반 400/404는 로그를 남기지 않고, 운영상 의미가 있는 403/409는 INFO, 502는 WARN, 500은 ERROR(스택 포함)로 기록
│     └─ (stable error code별 concrete 예외 18종 — InvalidSearchParameterException 등, openapi.yaml의 components.responses.* 기준. Librarian 관련 5종: LibrarianNotFoundException/LibrarianAccessDeniedException/LibrarianAlreadyOwnedException/InvalidLibrarianDataException/RepresentativeLibrarianNotSelectedException)
├─ health
│  └─ HealthController.java           # GET /health — 인증 불필요(SecurityConfig permitAll), k8s readiness/liveness probe 대상
├─ library                                # domain/application/infrastructure/web 4계층 서브패키지로 분리(기계적 리팩터링, 동작 변경 없음 — library 패키지가 40개 파일까지 커져 경계가 흐려지기 시작한 시점에 분리)
│  ├─ domain                             # 순수 도메인 모델. Spring 비의존(JPA/Hibernate 애노테이션은 예외 — entity 자체이므로), 포트/서비스가 이 계층을 참조
│  │  ├─ LibraryBook.java                # aggregate root(JPA entity), @SQLRestriction("deleted_at IS NULL") — register/updateMetadata/updateProgress/changeShelfRank/changeShelfId/softDelete에 불변식 캡슐화. genre(Genre)/readingStatus(ReadingStatus) 필드(네이티브 enum 매핑), totalPages는 Integer(nullable) — 없으면 progress()가 null
│  │  ├─ Genre.java                      # enum, 16종(NONE 포함) — genre_type 네이티브 enum과 1:1
│  │  ├─ ReadingStatus.java              # enum(PLANNED/READING/COMPLETED) — book_reading_status 네이티브 enum과 1:1. progress와 자동 연동되지 않는 독립 필드
│  │  ├─ Shelf.java                      # aggregate root(JPA entity), @SQLRestriction("deleted_at IS NULL") — create/rename/softDelete, isDefault는 서버 전용
│  │  ├─ ShelfRank.java                  # LexoRank pure 유틸(Spring 비의존) — initial/before/after/between/rebalancedSequence
│  │  ├─ ShelfRankExhaustedException.java  # 키 공간 소진 내부 신호(API 미노출) — rebalance 트리거용
│  │  └─ Scrap.java                      # aggregate root(JPA entity), @SQLRestriction("deleted_at IS NULL") — 독립 memberId 없음, LibraryBook을 통해서만 귀속. create/update/softDelete에 불변식(sentence·scrapImageUrl 필수, pageNumber>=1) 캡슐화
│  ├─ application                        # 유스케이스 서비스 + 포트(Repository 인터페이스). web이 참조하고, infrastructure가 포트를 구현
│  │  ├─ LibraryBookRepository.java      # 포트(순수 인터페이스, Spring Data 비의존) — 서비스 계층이 의존하는 도메인 메서드명. delete() 없음(soft delete로 통일)
│  │  ├─ LibraryBookService.java         # CRUD/reorder/moveShelf/progress 유스케이스, 소유권 검증(404→403 순서), shelfRank 계산·rebalance 오케스트레이션. deleteLibraryBook은 soft delete 후 ScrapService.softDeleteAllByBookId를 호출해 소속 Scrap을 벌크 soft delete(캐스케이드를 애플리케이션이 오케스트레이션, DB ON DELETE CASCADE 아님). 쓰기가 없는 메서드(getLibraryBooks/getLibraryBook)는 `@Transactional(readOnly = true)`
│  │  ├─ LibrarySortBy.java              # getLibraryBooks의 sortBy enum(SHELF_ORDER/TITLE/AUTHOR/CREATED_AT/PROGRESS) — web 컨트롤러가 쿼리 파라미터를 파싱해 이 타입으로 서비스에 전달
│  │  ├─ ShelfRepository.java            # 포트 — LibraryBookRepository와 동일한 패턴, delete() 없음
│  │  ├─ ShelfService.java               # getOrCreateDefaultShelf(동시성 처리)/createShelf/getShelves/updateShelf/deleteShelf(책 이동 후 soft delete)
│  │  ├─ ScrapRepository.java            # 포트 — findAllByBookId(캐스케이드 soft delete용) 포함, delete() 없음
│  │  └─ ScrapService.java               # createScrap/getScraps(책 스코프, LibraryBookNotFound/AccessDenied로 검증) / getScrap/updateScrap/deleteScrap(스크랩 스코프, soft delete) / softDeleteAllByBookId(package-private — LibraryBookService가 같은 application 패키지에서 캐스케이드용으로 호출)
│  ├─ infrastructure                     # 포트 구현체(JPA 어댑터). application의 포트를 구현하고 domain 엔티티를 다룸
│  │  ├─ LibraryBookJpaRepository.java   # Spring Data JPA 인터페이스(package-private) — 파생 쿼리 메서드명 + 필터/정렬용 @Query, 같은 서브패키지의 Adapter 내부에서만 사용
│  │  ├─ LibraryBookRepositoryJpaAdapter.java  # @Repository, LibraryBookRepository(포트) 구현 — LibraryBookJpaRepository로 위임, save()는 saveAndFlush로 위임해 unique 제약 위반을 호출 시점에 동기적으로 드러냄
│  │  ├─ ShelfJpaRepository.java         # Spring Data JPA 인터페이스(package-private)
│  │  ├─ ShelfRepositoryJpaAdapter.java  # @Repository, save()는 saveAndFlush로 위임(기본 책장 get-or-create 동시성 처리용)
│  │  ├─ ScrapJpaRepository.java         # Spring Data JPA 인터페이스(package-private) — findByBookIdOrderByCreatedAtAsc(페이징), findAllByBookId
│  │  └─ ScrapRepositoryJpaAdapter.java  # @Repository, save()는 saveAndFlush로 위임
│  └─ web
│     ├─ LibraryBookController.java      # POST/GET /api/v1/library/books, GET/PATCH/DELETE /{bookId}, /order, /shelf, /progress. memberId는 UUID(MemberIdResolver)
│     ├─ ShelfController.java            # POST/GET /api/v1/library/shelves, PATCH/DELETE /{shelfId}, GET /{shelfId}/books
│     ├─ ScrapController.java            # 클래스 레벨 @RequestMapping 없이 두 베이스 경로(POST/GET /api/v1/library/books/{bookId}/scraps, GET/PATCH/DELETE /api/v1/library/scraps/{scrapId})를 메서드별 전체 경로로 처리 — 이 저장소 최초의 다중 베이스 경로 컨트롤러
│     └─ dto                             # openapi.yaml 스키마 1:1 대응 record. Bean Validation 미도입 — 필수/불변식 검증은 도메인 계층의 IllegalArgumentException을 서비스가 잡아 concrete 예외로 번역하는 방식으로 통일(컨트롤러는 primitive 언박싱이 필요한 필드의 null만 직접 체크). LibraryBook 관련 DTO에 genre/readingStatus, Scrap 관련 DTO에 scrapImageUrl 포함
├─ discovery                              # ADR-0012(isbn 기반 검색)로 재설계 — library.application 포트(LibraryBookRepository)를 단방향 참조(반대 방향 없음)
│  ├─ ExternalBook.java                  # record — 포트가 반환하는 공용 표현(외부 API 벤더 비의존), 컨트롤러가 그대로 응답에 사용
│  ├─ BookSearchResult.java              # record(libraryBook, book) — alreadyRegistered/found/notFound 팩토리. libraryBook이 있으면 이미 등록된 것
│  ├─ BookDiscoveryClient.java           # 포트(순수 인터페이스) — Optional<ExternalBook> lookup(isbn) + List<ExternalBook> searchByTitle/searchByAuthor(ADR-0013)
│  ├─ BookDiscoveryService.java          # search(memberId, isbn): isbn 검증 후 LibraryBookRepository.findByMemberIdAndIsbn로 먼저 서재를 조회 — 있으면 알라딘 호출 없이 alreadyRegistered, 없으면 포트(lookup)에 위임. searchByTitleAndAuthor(title, author): 둘 중 하나라도 공백이면 400(INVALID_SEARCH_PARAMETER), 아니면 포트의 searchByTitle/searchByAuthor를 각각 호출해 isbn 교집합의 제목검색 순서상 최상단 1권을 Optional로 반환(memberId·서재 조회 불필요). 생성자 주입 지점에 @Lazy(아래 aladin 패키지 참조)
│  ├─ aladin                             # 알라딘 API 연동 구현 세부사항 — 전부 package-private, discovery 패키지 밖에서는 BookDiscoveryClient 포트만 보인다
│  │  ├─ AladinBookDiscoveryClient.java  # @Component + @Lazy, RestClient로 ItemLookUp(isbn 단건, isbn 길이로 ItemIdType을 ISBN/ISBN13 분기)과 ItemSearch(QueryType=Title/Author, MaxResults=50 — ADR-0013)를 호출(둘 다 라이브 호출로 확인). HTTP 200이어도 응답 바디의 errorCode가 있으면 AladinApiException(502) — 단, ItemLookUp의 errorCode 8("키에 해당하는 상품이 존재하지 않습니다")만은 예외이며 "찾지 못함"(Optional.empty())으로 처리한다. ItemSearch는 결과 없음을 빈 item 배열로 돌려주므로(errorCode 없음) 그대로 빈 리스트가 된다 — 라이브 호출로 확인
│  │  ├─ AladinSearchResponse.java       # ItemLookUp·ItemSearch 공용 응답 DTO(record) — 미매핑 필드가 오면 역직렬화 실패(엄격 검증, ignoreUnknown 미사용)
│  │  ├─ AladinItem.java                 # 응답 항목 DTO(record)
│  │  ├─ AladinSubInfo.java              # itemPage(총 페이지) 등 optResult로 요청하는 부가 필드 — 실무에서 거의 항상 비어 있음(라이브 호출로 확인)
│  │  └─ AuthorNameNormalizer.java       # "이름 (역할), 이름 (역할)" → "이름, 이름" 변환 순수 유틸
│  └─ web
│     ├─ BookDiscoveryController.java    # GET /api/v1/books/search?isbn= (memberId 필요), GET /api/v1/books/search/by-title-author?title=&author= (ADR-0013, memberId 불필요 — 인증만)
│     └─ dto
│        ├─ BookSearchResponse.java              # { alreadyRegistered, libraryBook?(library.web.dto.LibraryBookDetailResponse 재사용), book?(ExternalBook) }
│        └─ TitleAuthorBookSearchResponse.java   # { book?(ExternalBook) } — 교집합 최상단 1권, 없으면 null
├─ librarian                             # library와 동일한 4계층(domain/application/infrastructure/web) 구조. 회원 소유 사서 인스턴스 모델로 전면 재작성(ADR-0011, ADR-0009 대체) — 마스터 카탈로그 조회만 하던 이전 구조에서 획득/개명/방출/대표지정까지 확장
│  ├─ domain
│  │  ├─ Librarian.java                  # aggregate root(JPA entity), @SQLRestriction("deleted_at IS NULL") — memberId(UUID)/type/name/level/experience/isRepresentative. acquire/rename/markAsRepresentative/unmarkAsRepresentative/softDelete 도메인 메서드
│  │  ├─ LibrarianType.java              # enum(RUSSIAN_BLUE/SHOEBILL) — librarian_type 네이티브 enum과 1:1
│  │  └─ LibrarianTypeInfo.java          # JPA entity, 마스터 데이터(타입별 imageUrl/clickedImageUrl) — Flyway 시드 전용, 도메인 메서드 없이 @Getter만 + 테스트 픽스처용 public 생성자. PK가 LibrarianType 자체(네이티브 enum)
│  ├─ application
│  │  ├─ LibrarianRepository.java        # 포트 — save/findById/findAllOwned/existsByMemberIdAndType/findRepresentative
│  │  ├─ LibrarianTypeInfoRepository.java  # 포트 — findAll
│  │  └─ LibrarianService.java           # getLibrarianTypes/acquireLibrarian(타입 중복 시 409)/getLibrarians/renameLibrarian/selectRepresentative(기존 대표 자동 해제)/getRepresentative(미선택 시 404)/deleteLibrarian(soft delete). getOwnedLibrarian으로 404→403 순서 통일
│  ├─ infrastructure
│  │  ├─ LibrarianJpaRepository.java / LibrarianRepositoryJpaAdapter.java
│  │  └─ LibrarianTypeInfoJpaRepository.java / LibrarianTypeInfoRepositoryJpaAdapter.java
│  └─ web
│     ├─ LibrarianController.java        # GET /api/v1/librarian-types, POST/GET /api/v1/librarians, PATCH/DELETE /{librarianId}, PATCH /{librarianId}/representative, GET /representative
│     └─ dto                             # AcquireLibrarianRequest/Response, LibrarianListResponse/LibrarianSummary, RenameLibrarianRequest/Response, RepresentativeLibrarianResponse, LibrarianTypeListResponse/LibrarianTypeSummary — openapi 스키마 1:1
│  ※ LibrarianLevel(레벨별 필요 경험치 정책)은 JPA 엔티티로 만들지 않았다 — 레벨업 로직이 범위 밖이라 앱 코드가 조회하지 않는다. DB 테이블/FK만 V9 마이그레이션으로 존재(YAGNI, 필요해지면 추가)
└─ security
   ├─ SecurityConfig.java             # JwtDecoder/SecurityFilterChain/AuthenticationEntryPoint 빈 + @Order(0) actuatorSecurityFilterChain(/actuator/prometheus·/actuator/health 무인증 — 관리 포트 8081도 같은 필터 체인을 타므로, CLIAR-255)
   ├─ JwtAuthenticationEntryPoint.java
   ├─ MemberIdResolver.java           # Jwt의 sub 클레임 → UUID.fromString()으로 memberId 추출하는 단일 지점(2026-08-25~, 이전엔 String 그대로 반환) — sub이 유효한 UUID 형식이 아니면 IllegalArgumentException
   └─ jwt
      ├─ CognitoAccessTokenValidator.java  # 검증 기준 조합(issuer+만료 / token_use / client_id) — SecurityConfig와 테스트가 같은 객체를 쓰게 하는 단일 지점
      ├─ TokenUseValidator.java       # token_use == access
      └─ ClientIdValidator.java       # client_id == backend-auth의 Cognito Backend App Client

src/main/resources
├─ application.yaml          # 공통 설정 (JPA, Flyway 활성화, OAuth2 Resource Server issuer-uri/app-client-id, book-service.aladin.ttb-key=${ALADIN_API_TTB_KEY}, 관측 기본값: 트레이스 export 꺼짐·OTLP 메트릭 export 꺼짐·sampling 1.0·JDBC 파라미터 값 미기록·actuator web 노출 0개, Micrometer 공통 태그 application=backend-book, http.server.requests 히스토그램 켜짐)
├─ application-local.yaml    # 로컬 프로필 — docker-compose Postgres 기본값
├─ application-prod.yaml     # 운영 프로필(배포는 dev·prod 둘 다 이 프로필) — datasource는 전부 env var, JSON 구조화 로그 + 트레이스 export 켜짐
├─ static
│  └─ docs
│     └─ index.html          # Swagger UI 진입 페이지 — webjar(swagger-ui) 자산을 로드해 /openapi.yaml을 렌더링. /openapi.yaml 자체는 여기 없고 build.gradle의 processResources가 docs/api/openapi.yaml을 빌드 시점에 static/openapi.yaml로 복사해 채운다(수동 사본 없음)
└─ db/migration
   ├─ V1__init.sql                     # baseline (빈 마이그레이션)
   ├─ V2__create_library_book.sql      # library_book 테이블 + unique 제약(member_id+isbn partial — isbn 없는 도서는 중복판정 안 함, ADR-0007)
   ├─ V3__add_shelf_and_rescope_library_book.sql  # shelf 테이블(+ 기본 책장 부분 unique 인덱스) 신설, library_book.shelf_id 추가(+ FK), shelfRank unique 제약을 member_id 전역→shelf_id 범위로 재조정(ADR-0008). V2는 develop에 이미 병합되어 직접 수정하지 않음
   ├─ V4__create_scrap.sql             # scrap 테이블 신설, book_id FK에 ON DELETE CASCADE(책 삭제 시 스크랩도 함께 삭제, CLIAR-45)
   ├─ V5__create_librarian.sql          # librarian 테이블(마스터, PK만 — 앱이 아닌 이 마이그레이션이 직접 INSERT) + member_librarian_selection 테이블(member_id unique, librarian_id FK), 시드 데이터 2종(CLIAR-46)
   ├─ V6__drop_member_librarian_selection.sql  # 대표 사서 선택이 Member 서비스로 이관되어 member_librarian_selection 테이블 DROP(ADR-0009, 이후 ADR-0011로 재반전). librarian(마스터)은 유지
   ├─ V7__rescope_shelf_and_library_book.sql  # shelf/library_book PK를 id로 리네이밍, member_id UUID화, deleted_at 추가(soft delete), genre_type/book_reading_status 네이티브 enum 생성 + genre/reading_status 컬럼 추가, total_pages nullable화, cover_url TEXT화(ADR-0010)
   ├─ V8__rescope_scrap.sql             # scrap PK를 id로 리네이밍, deleted_at 추가, scrap_image_url 필수 컬럼 추가(기존 행은 빈 문자열로 백필 후 NOT NULL 전환), book_id FK의 ON DELETE CASCADE 제거(soft delete로 전환하며 애플리케이션이 캐스케이드 오케스트레이션)
   └─ V9__redesign_librarian.sql        # 기존 librarian(마스터) DROP 후 librarian_type enum → librarian_type_info → librarian_level(level=1,required_experience=0 최소 시드) → librarian(회원 소유 인스턴스) 순으로 재생성(ADR-0011, 사용자 제공 SQL의 테이블 생성 순서 오류를 바로잡음)

src/test/java/com/chc/dpgb
├─ common/exception/GlobalExceptionHandlerTest.java
├─ common/logging/JsonLogFormatterTest.java       # JSON 로그의 필드명 계약(7개 필수 필드)·MDC→trace_id/span_id·예외 렌더링
├─ common/observability/ObservabilityConfigurationTest.java  # sampling/OTLP 설정 YAML 계약 + ObservationPredicate 경로 제외(/actuator 포함) + Prometheus 노출(dev만·별도 관리 포트) YAML/kustomize 계약 단위 테스트
├─ common/observation/RecordingObservationHandler.java  # 테스트 픽스처(테스트 아님) — custom span 검증용 기록 핸들러
├─ discovery/BookDiscoverySpanTest.java           # book.discovery.search span 이름·outcome 3종·오류 기록
├─ library/application/ShelfRebalanceSpanTest.java  # library.shelf.rebalance span 이름·book_count 속성
├─ library/domain/ShelfRankTest.java
├─ library/domain/LibraryBookTest.java
├─ library/domain/ScrapTest.java                 # 도메인 unit — sentence/scrapImageUrl/pageNumber 불변식, softDelete
├─ library/application/ShelfServiceTest.java      # Mockito 기반 애플리케이션 서비스 단위 테스트 — get-or-create 동시성, 소유권 404/403
├─ library/application/LibraryBookServiceTest.java  # Mockito 기반 — shelf 해석, 중복 409, reorder 검증/rebalance, moveShelf, 책 삭제 시 ScrapService.softDeleteAllByBookId 호출 검증
├─ library/application/ScrapServiceTest.java      # Mockito 기반 — 책 스코프/스크랩 스코프 소유권 403/404 각각 검증, softDeleteAllByBookId 단위 테스트
├─ library/web/LibraryBookControllerTest.java  # @WebMvcTest — SecurityConfigTest 패턴 재사용, 컨트롤러 자체 검증(필수 필드 누락 400 등) 위주
├─ library/web/ShelfControllerTest.java        # @WebMvcTest
├─ library/web/ScrapControllerTest.java        # @WebMvcTest
├─ discovery/BookDiscoveryServiceTest.java      # Mockito — isbn 검색 3분기 + 제목·저자 교집합/빈결과/파라미터 400(ADR-0013)
├─ discovery/web/BookDiscoveryControllerTest.java  # @WebMvcTest — /search, /search/by-title-author 각각 200/400/401/502
├─ discovery/aladin/AuthorNameNormalizerTest.java  # 순수 함수 단위 테스트
├─ discovery/aladin/AladinBookDiscoveryClientTest.java  # MockRestServiceServer(네트워크 미사용) — fixture는 ItemLookUp/ItemSearch 라이브 호출로 캡처한 실제 응답(CLIAR-161, CLIAR-242) + errorCode 8("찾지 못함") 응답
├─ librarian/application/LibrarianServiceTest.java      # Mockito — 타입 카탈로그/획득(중복 409)/목록/개명/대표지정(기존 대표 해제 포함)/대표조회(미선택 404)/방출
├─ librarian/web/LibrarianControllerTest.java           # @WebMvcTest — 7개 endpoint 전수
└─ security/...  # validator/MemberIdResolver(UUID 반환) 단위 테스트, SecurityConfigTest

src/integrationTest/java/com/chc/dpgb
├─ TestcontainersConfiguration.java       # @TestConfiguration, PostgreSQLContainer + @ServiceConnection, withReuse(true)
├─ IntegrationTestSupport.java            # @SpringBootTest + TestcontainersConfiguration import
├─ RepositoryIntegrationTestSupport.java  # @DataJpaTest + AutoConfigureTestDatabase(NONE) + @ImportAutoConfiguration(FlywayAutoConfiguration) + TestcontainersConfiguration import
├─ DpgbApplicationTests.java              # IntegrationTestSupport 상속 (smoke test)
├─ observability/PrometheusMetricsExposureTest.java  # IntegrationTestSupport + @AutoConfigureMockMvc — /actuator/prometheus 스크레이프에 http_server_requests_seconds_count/_bucket + application="backend-book" 확인
├─ library/infrastructure/LibraryBookRepositoryTest.java # RepositoryIntegrationTestSupport 상속 — 저장/조회, unique 제약(shelf_id 범위), FK
├─ library/infrastructure/ShelfRepositoryTest.java       # RepositoryIntegrationTestSupport 상속 — 기본 책장 unique 제약, 목록 정렬
├─ library/infrastructure/ScrapRepositoryTest.java       # RepositoryIntegrationTestSupport 상속 — 책별 목록 정렬, findAllByBookId 조회(구 "책 삭제 시 DB cascade 삭제" 테스트는 ON DELETE CASCADE 제거로 전제가 사라져 제거 — 캐스케이드는 이제 서비스 단위 테스트로 검증)
├─ librarian/infrastructure/LibrarianRepositoryTest.java # RepositoryIntegrationTestSupport 상속 — 획득/조회, 회원당 동일 타입 유일성, 회원당 대표 사서 유일성(uk_librarian_member_representative), 대표 사서 조회. 구 마스터 카탈로그 시드 조회 테스트는 V9가 그 테이블을 DROP해 전면 폐기
└─ librarian/infrastructure/LibrarianTypeInfoRepositoryTest.java  # RepositoryIntegrationTestSupport 상속 — V9 시드 데이터(RUSSIAN_BLUE/SHOEBILL) 조회

src/integrationTest/resources
└─ testcontainers.properties  # testcontainers.reuse.enable=true

docker-compose.yml  # 로컬 개발용 PostgreSQL (POSTGRES_DB/USER/PASSWORD=dpgb)
```

## 비밀값

`AUTH_ISSUER_URI`/`AUTH_APP_CLIENT_ID`/`ALADIN_API_TTB_KEY` 모두 env var로만 주입하고 `application*.yaml`에 기본값을 두지 않는다. 저장소 루트의 `.env`는 `.gitignore`에 등록되어 있지만, 이 애플리케이션은 `.env` 파일을 자동으로 읽지 않는다(dotenv 라이브러리 미도입) — 로컬 실행 시 셸 export나 IDE 실행 설정으로 실제 프로세스 환경변수에 주입해야 한다. 자격 증명이 필요한 빈(`SecurityConfig.jwtDecoder`, `AladinBookDiscoveryClient`)은 값이 없는 환경(CI, 다른 개발자)에서도 앱이 기동되도록 빈과 그 주입 지점 양쪽에 `@Lazy`를 붙인다 — 하나만 붙이면 다른 즉시 생성되는 빈의 생성자 인자로 해석될 때 여전히 즉시 생성된다(CLIAR-28에서 처음 발견, CLIAR-34에서 재확인).

## 서비스 경계

이 저장소는 Book Service(Java, 이 프로젝트)이며, 독립된 Python RAG Service와 별도로 개발된다.

Book Service는 database-per-service 원칙에 따라 자신만의 데이터베이스를 소유한다(`.harness/DECISIONS.md` 참조 — 2026-08-20에 한 차례 "Java 서비스 전체가 하나의 DB를 공유"로 바뀌었다가, MSA 원칙에 맞게 다시 서비스별 분리로 되돌렸다). 다른 Java MSA 서비스는 물론 Python RAG Service의 schema도 직접 JOIN할 수 없고, 모든 서비스 간 데이터 공유는 API 또는 event로만 한다. Python RAG Service는 지금처럼 자체 PostgreSQL + pgvector를 별도로 소유한다.

**실제 구현 형태(2026-08-30~)**: 서비스별 **데이터베이스** 분리이고 클러스터·인스턴스 분리는 아니다. 여러 서비스가 같은 Aurora PostgreSQL 클러스터를 공유하되 각자 자기 데이터베이스만 사용한다 — dev는 `dpyb-dev` 클러스터의 `dpyb_book`, prod는 `dpyb-prod` 클러스터의 `dpyb_book`이다(둘 다 role `admin` 소유). PostgreSQL은 데이터베이스 간 JOIN이 `dblink`/`postgres_fdw` 없이 불가능하므로 위 "직접 JOIN할 수 없다" 제약이 엔진 차원에서 강제되고, 클러스터를 늘리지 않아 인스턴스 고정비가 배수로 늘지 않는다. 2026-08-30 이전에는 세 서비스(auth·record·book) 테이블이 단일 `dpyb` 데이터베이스의 `public` 스키마 하나에 섞여 있었다. `CLAUDE.md`의 DB 정책 문구는 "인스턴스·데이터베이스"를 함께 명시하고 있어 이 형태와 엄밀히는 어긋난다 — 문구 조정 여부는 `.harness/BACKLOG.md` 참조.

## 테스트 구조

- `test`: 단위 테스트. DB 없음. `com.chc.dpgb.security` 패키지의 validator/`MemberIdResolver` 단위 테스트와 `SecurityConfigTest`/`GlobalExceptionHandlerTest`(`@WebMvcTest` + 테스트 전용 nested 컨트롤러), `com.chc.dpgb.library.domain`의 `ShelfRankTest`/`LibraryBookTest`/`ScrapTest`(Domain unit, Spring 컨텍스트 없음)가 있다.
- `integrationTest`: PostgreSQL Testcontainers 기반 통합 테스트. Gradle에 구성 완료 — `./gradlew integrationTest`로 단독 실행, `./gradlew check`가 `test`와 함께 실행. Docker(Docker Desktop 등)가 로컬에 떠 있어야 한다.
- `RepositoryIntegrationTestSupport`(`@DataJpaTest`)가 CLIAR-31에서 처음 만들어졌다. `@DataJpaTest`의 큐레이션된 autoconfiguration 목록은 Flyway를 포함하지 않으므로 `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`를 명시적으로 추가해야 `ddl-auto: validate`가 마이그레이션된 실제 스키마를 검증한다(`.harness/DECISIONS.md` 참조). 새 `*RepositoryImpl`을 추가할 때 이 기반 클래스를 상속한다. DB 레벨 `ON DELETE CASCADE` 같은 부수효과를 검증할 때는 Hibernate 1차 캐시가 낡은 상태를 들고 있을 수 있어 `TestEntityManager`(`org.springframework.boot.jpa.test.autoconfigure`)의 `clear()`로 캐시를 비운 뒤 재조회해야 한다(`ScrapRepositoryTest`, CLIAR-45에서 확인).
- 통합 테스트: `DpgbApplicationTests`(`IntegrationTestSupport` 상속, 빈 smoke test), `LibraryBookRepositoryTest`/`ShelfRepositoryTest`/`ScrapRepositoryTest`/`LibrarianRepositoryTest`/`LibrarianTypeInfoRepositoryTest`(`RepositoryIntegrationTestSupport` 상속).

## 관측 (로깅 · 분산 트레이싱)

Kubernetes에서 `stdout JSON 로그 → Grafana Alloy → Loki`, `OTLP → OpenTelemetry Collector → Tempo` 두 경로로 관측한다. 애플리케이션의 책임은 "JSON 한 줄을 stdout에 쓴다"와 "OTLP로 span을 내보낸다"까지이고, 수집기·저장소는 인프라 저장소 소관이다.

**로깅**

- 로그는 파일이 아니라 **stdout으로만** 나간다(별도 appender 설정 없음 = Boot 기본 콘솔).
- 배포 환경(prod 프로필)은 `logging.structured.format.console`에 `com.chc.dpgb.common.logging.JsonLogFormatter`를 지정해 JSON 한 줄로 출력한다. 로컬(local 프로필)은 Boot 기본 평문 로그 그대로다.
- 필드: `timestamp`(ISO-8601 UTC) / `level` / `service` / `logger` / `message` / `thread` / `trace_id` / `span_id`, 예외가 있으면 `exception`·`stack_trace`, 그 외 허용된 MDC 키는 top-level로 통과. `memberId`/`sub`/`cognitoSub` 같은 지속적 사용자 식별자와 token/password/Authorization 계열 MDC 키는 출력하지 않는다. **필드명 계약의 소유자는 이 클래스**이고 Loki 쿼리가 이 이름에 의존한다. Boot 내장 `ecs`/`logstash` 포맷을 쓰지 않은 이유는 이 필드명 때문이다(`DECISIONS.md`).
- `service` 값은 `OTEL_SERVICE_NAME` → `spring.application.name` 순으로 해석해 OTel resource의 `service.name`과 항상 같은 값이 되게 한다.
- `trace_id`/`span_id`는 micrometer-tracing이 MDC에 넣는 `traceId`/`spanId`에서 온다. 트레이스 컨텍스트가 없는 로그(기동 로그 등)에서는 두 필드가 빠진다.
- **로그를 남기는 지점(이벤트 중심, 컨트롤러 일괄 삽입 없음)**: `GlobalExceptionHandler`(일반 400/404는 미기록, 403/409 INFO, 502 WARN, 500 ERROR), `AladinBookDiscoveryClient`(외부 API 실패 WARN), `LibraryBookService`(책 등록·삭제 INFO / 등록 경합·랭크 재정렬 WARN), `ShelfService`(기본 책장 경합 WARN / 책장 삭제 INFO), `LibrarianService`(사서 획득·대표 교체 INFO). 비즈니스 로그는 `bookId`/`shelfId`/`librarianId`/결과 code 중심으로 남기고 `memberId`/Cognito `sub` 원문은 남기지 않는다.
- **민감정보·사용자 식별자 금지**: password·access/refresh/ID token·`Authorization` 헤더·알라딘 TTBKey·AWS credential을 어떤 로그에도 남기지 않는다. `memberId`/Cognito `sub`는 credential은 아니지만 지속적 사용자 식별자이므로 기본적으로 stdout 로그와 span attribute에 남기지 않는다. 특히 `AladinBookDiscoveryClient`는 요청 URI에 TTBKey가 들어 있어 **URI 자체를 로그하지 않는다**. request/response body를 통째로 남기는 필터도 두지 않는다. JDBC span도 `jdbc.datasource-proxy.include-parameter-values: false`로 바인딩 파라미터 값을 기록하지 않는다.

**트레이싱**

- Spring Boot 4.1의 `spring-boot-opentelemetry`가 `OTEL_*` 표준 환경변수를 Spring 프로퍼티로 매핑한다(`OpenTelemetryEnvironmentVariableEnvironmentPostProcessor`). 그래서 OTLP 엔드포인트를 코드나 yaml에 하드코딩하지 않고 ConfigMap의 환경변수로만 준다. `OTEL_EXPORTER_OTLP_ENDPOINT`에는 `v1/traces` 경로를 Boot가 자동으로 덧붙이므로 베이스 URL(`http://host:4318`)만 준다.
- **자동 계측 구간**: inbound HTTP/Spring MVC(`WebMvcObservationAutoConfiguration`), RestClient outbound(`RestClientObservationAutoConfiguration` — `AladinBookDiscoveryClient`가 주입받는 `RestClient.Builder`에 적용), JDBC/PostgreSQL(`DataSourceObservationAutoConfiguration`, datasource-micrometer). 넷 다 실행 중인 앱의 condition report로 확인했다.
- **서버 관측 제외 경로**(CLIAR-234 + 관측-인프라-연동): `com.chc.dpgb.common.observability.ObservabilityConfiguration`의 `ObservationPredicate` 빈이 inbound HTTP 관측에서 `/health`·`/docs`·`/webjars`·`/openapi.yaml`·`/actuator`(정확 일치 또는 바로 아래 하위 경로)를 제외한다 — k8s 프로브·ALB healthcheck·Swagger 자산·Prometheus 스크레이핑 요청이 span·`http.server.requests` 메트릭을 만들지 않는다. `/actuator` 외 4개는 `SecurityConfig` 메인 체인의 `permitAll` 목록과 같다. `/actuator`는 메인 체인이 아니라 전용 `actuatorSecurityFilterChain`(`/actuator/prometheus`·`/actuator/health` permitAll)이 담당한다 — 관측 제외는 접두사 `/actuator` 전체이고 무인증 허용은 그 두 경로만이라 범위가 다르다. RestClient outbound·JDBC·커스텀 span은 필터하지 않는다.
- **전파**: `management.tracing.propagation.produce` 기본값이 `[W3C]`라 다른 MSA 호출 시 `traceparent`가 자동으로 실린다(수신은 W3C/B3/B3_MULTI 모두 허용).
- **직접 추가한 span은 2개뿐**이다. 자동 계측으로 설명되는 구간에는 수동 span을 넣지 않는다.
  - `book.discovery.search`(`BookDiscoveryService`) — 속성 `book.discovery.outcome`(`ALREADY_REGISTERED`/`FOUND`/`NOT_FOUND`). 서재에 이미 있으면 알라딘을 호출하지 않아, 이 속성이 없으면 trace에 외부 호출 span이 있는 요청과 없는 요청이 이유 없이 섞여 보인다.
  - `library.shelf.rebalance`(`LibraryBookService`) — 속성 `library.shelf.book_count`(값 종류가 많아 high cardinality). 드물게 한 요청이 책장 전체를 다시 저장하는 이유를 드러낸다.
  - 두 span은 Micrometer `ObservationRegistry`로 만든다. 단위 테스트에서는 `ObservationRegistry.NOOP` 또는 `RecordingObservationHandler`(테스트 픽스처)를 주입한다.
- **export 실패는 요청 실패로 이어지지 않는다.** `BatchSpanProcessor`가 비동기로 내보내므로, Collector를 내린 상태에서 요청을 계속 보내도 전부 200이고 로그에는 exporter의 `Failed to export spans`만 남는다(실측 확인).
- OTLP **push** 메트릭(`micrometer-registry-otlp`, starter가 함께 끌고 옴)은 `management.otlp.metrics.export.enabled: false`로 꺼 뒀다. 메트릭은 아래 Prometheus **pull**로만 받는다.

**메트릭 (Prometheus, 관측-인프라-연동)**

- infra Prometheus가 `/actuator/prometheus`(Micrometer 포맷, `spring-boot-starter-actuator` + `micrometer-registry-prometheus`)를 스크레이핑한다. infra의 HTTP 5xx 에러율·p99 레이턴시 알림이 `http_server_requests_seconds_count`/`_bucket`에 의존한다.
- **공통 `application.yaml`**: `management.endpoints.web.exposure.include`를 빈 값으로 둬 어떤 actuator 웹 엔드포인트도 노출하지 않는다(기본값 `health`조차). `management.metrics.tags.application: backend-book`(모든 메트릭 공통 라벨 — infra 쿼리 키, `OTEL_SERVICE_NAME`·로그 `service`와 동일값), `management.metrics.distribution.percentiles-histogram.http.server.requests: true`(p99 알림용 `_bucket` 생성).
- **dev overlay만** 실제로 노출한다: `MANAGEMENT_SERVER_PORT=8081`(별도 관리 포트 — 메인 8080은 ALB로 인터넷 노출되므로 분리), `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,prometheus`. **관리 포트는 자식 컨텍스트로 뜨지만 Spring Boot의 `ServletManagementChildContextConfiguration`이 부모의 `springSecurityFilterChain`을 그 컨텍스트에 재등록하므로, 8081도 메인 포트와 같은 `SecurityConfig`의 필터 체인을 탄다(CLIAR-255에서 8081 스크레이핑이 401로 확인됨).** 그래서 `SecurityConfig`에 `@Order(0)` `actuatorSecurityFilterChain`(`securityMatcher("/actuator/prometheus", "/actuator/health")` + `permitAll`)을 두어 이 두 경로만 무인증 허용한다 — `EndpointRequest.toAnyEndpoint()`는 관리 포트 분리 시 부모 컨텍스트에서 매칭되지 않아 쓰지 않는다. 나머지 `/actuator/*`는 메인 체인으로 떨어져 401이 유지되고, ALB Ingress는 8080만 노출해 외부에서 닿지 않는다. prod overlay·base 매니페스트는 불변(`kubectl kustomize` diff 0).
- prod는 이 저장소 범위 밖이다 — Collector/Prometheus가 prod 클러스터에 준비되면 dev와 같은 패턴(관리 포트 env + Service metrics 포트 patch + ServiceMonitor)을 prod overlay에 이식한다(`BACKLOG.md`).

**환경별 스위치**

| 상황 | 트레이스 export | 로그 포맷 | Prometheus 메트릭 |
| --- | --- | --- | --- |
| 로컬(local 프로필) · 테스트(프로필 없음) | 꺼짐(`management.tracing.export.enabled: false`, 공통 기본값), sampling `1.0` | 사람이 읽는 평문 | actuator web 노출 0개 |
| dev k8s(prod 프로필) | 켜짐 + `OTEL_EXPORTER_OTLP_*` 환경변수, `MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0` | JSON | `/actuator/prometheus` on 관리 포트 8081 + ServiceMonitor |
| prod k8s(prod 프로필) | 켜짐 + `OTEL_EXPORTER_OTLP_*` 환경변수, `MANAGEMENT_TRACING_SAMPLING_PROBABILITY=0.1` | JSON | 미노출(범위 밖) |

export가 꺼져 있거나 sampling probability를 낮춰도 W3C trace context propagation은 계속 동작한다. Collector 없이도 로컬 로그를 trace_id로 묶어 볼 수 있다. **로컬에서 Collector 없이 기동해 WARN/ERROR 0건인 것을 실측 확인했다.**

## DB 문서

- ERD(DBML): `docs/db/erd.dbml` — `db/migration`의 V1~V9 적용 후 최종 스키마 스냅샷. 마이그레이션 변경 시 수동으로 함께 갱신해야 한다(자동 동기화 없음). dbdiagram.io 등에 붙여넣으면 다이어그램으로 볼 수 있다.

## API 문서

- wire 계약: `docs/api/openapi.yaml`
- 사용 안내: `docs/api/README.md`
- 계약 결정: `docs/api/decisions/`
- 실행 중인 앱에서 브라우저로 열람: `/docs/index.html`(Swagger UI, 인증 불필요) — `docs/api/openapi.yaml`을 그대로 렌더링만 한다(별도 스펙 생성 없음)

## 배포 (EKS)

`backend-record`와 동일한 GitOps 패턴을 그대로 이식했다: GitHub Actions가 ECR에 이미지를 빌드/푸시하고 dev overlay의 이미지 태그를 갱신하는 커밋을 push하면, ArgoCD가 그 변경을 감지해 자동 동기화한다. 애플리케이션 코드가 클러스터를 직접 건드리지 않는다(push-to-deploy가 아니라 pull 기반).

- ECR: `594532711953.dkr.ecr.ap-northeast-2.amazonaws.com/dpyb-dev/dpyb-book`, EKS 클러스터: `dpyb-dev`
- `.github/workflows/build-push-ecr.yml`: `develop` push(또는 수동 `workflow_dispatch`) 시 `Dockerfile`로 이미지를 빌드해 SHA 태그 + `develop-latest` 태그로 ECR에 푸시하고, `k8s/overlays/dev/kustomization.yaml`의 `newTag`를 SHA로 갱신하는 커밋을 같은 브랜치에 push한다(`paths-ignore: k8s/**`로 이 커밋 자체가 워크플로우를 다시 트리거하는 무한루프를 막는다). `main` push → prod도 활성화되어 있다: "Resolve target by branch" 스텝이 브랜치별로 대상을 나눠 `develop`은 `dpyb-dev/dpyb-book`(MUTABLE, `develop-latest` movable 태그 push) + dev overlay를, `main`은 `dpyb-prod/dpyb-book`(IMMUTABLE, 커밋 SHA 태그만 push — movable 태그 skip) + prod overlay를 갱신한다. 빌드는 `docker/setup-buildx-action` + `docker/build-push-action`으로 **멀티아키(`linux/amd64,linux/arm64`)** 이미지를 만든다 — buildx 멀티플랫폼 빌드는 로컬 이미지가 남지 않아 `docker tag`를 쓸 수 없으므로, push할 태그 전부를 "Resolve target by branch" 스텝이 `tags` 출력으로 계산해 한 번에 넘긴다. `provenance: false`로 attestation(unknown/unknown) 매니페스트가 붙지 않게 한다.
- `k8s/`: Kustomize 기반. `base/`(Deployment/Service/Ingress/ConfigMap 공통 정의, namespace/replicas/image태그는 두지 않음) + `overlays/dev/`(namespace `dpyb-book-dev`, replicas 1, image `newTag: develop-latest`를 CI가 커밋 SHA로 갱신, `configmap-patch.yaml`로 dev Cognito 값 주입, **관측-인프라-연동**: `deployment-patch.yaml`/`service-patch.yaml`로 관리 포트 8081(`management`/`metrics`) 추가 + `servicemonitor.yaml`로 Prometheus `ServiceMonitor` 추가 — 전부 dev 전용) + `overlays/prod/`(활성. namespace `dpyb-book`, replicas 2, image `newName: dpyb-prod/dpyb-book`·`newTag`를 CI가 커밋 SHA로 갱신, `configmap-patch.yaml`로 prod Cognito 값 주입 — 메트릭 노출·관리 포트 없음). 상용은 book 전용 노드 분리를 위해 `nodepin-patch.yaml`로 `nodeSelector workload=book` + `toleration dedicated=book:NoSchedule`을 얹고, kustomization의 `patches`에서 이를 참조한다. `k8s/cluster/nodepool-book.yaml`은 이 상용 노드 분리를 뒷받침하는 Karpenter(EKS Auto Mode) NodePool로, `workload=book` label과 `dedicated=book:NoSchedule` taint를 가진 노드를 프로비저닝한다 — dev 클러스터에는 적용하지 않고 `dpyb-prod` 컨텍스트에서 `kubectl apply`로 1회 수동 적용한다(GitOps 대상 아님). 이 NodePool은 인스턴스 카테고리(`t`/`m`)만 제약하고 아키텍처는 제약하지 않으며, 실제로 프로비저닝된 `dpyb-prod` 노드는 전부 **arm64(Graviton)**다(`t4g.medium`/`c6g.large`). `dpyb-dev`는 amd64(`r5a`/`c5a`)와 arm64(`c6g`)가 섞여 있고 dev overlay에는 `nodeSelector`가 없어 파드가 어느 쪽에도 착지할 수 있다 — 그래서 컨테이너 이미지는 항상 멀티아키여야 한다. `k8s/secret.example.yaml`은 실제 값 없는 구조 예시일 뿐이고, 실제 Secret(`backend-book-secret`: `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`/`ALADIN_API_TTB_KEY`)은 Git에 커밋하지 않고 `kubectl create secret` 또는 SealedSecrets/External Secrets로 클러스터에 직접 생성한다.
- ConfigMap(`backend-book-config`)에는 관측용 `OTEL_SERVICE_NAME=backend-book`·`OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf`(base)와 overlay별 `OTEL_EXPORTER_OTLP_ENDPOINT`·`OTEL_RESOURCE_ATTRIBUTES=deployment.environment.name={dev|prod}`·`MANAGEMENT_TRACING_SAMPLING_PROBABILITY`(dev `1.0`, prod `0.1`)도 함께 둔다. dev의 Collector 주소는 `http://otel-collector.monitoring.svc.cluster.local:4318`(infra 확정값), prod는 아직 관례 기본값(`http://opentelemetry-collector.observability...`)이라 교체 대상이다(`BACKLOG.md`). dev overlay는 추가로 Prometheus 메트릭용 `MANAGEMENT_SERVER_PORT=8081`·`MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,prometheus`와, Collector 계약 명시용 `OTEL_METRICS_EXPORTER=none`·`OTEL_LOGS_EXPORTER=none`을 둔다(prod에는 없음). 그 밖에는 민감하지 않은 값만 둔다: `SPRING_PROFILES_ACTIVE=prod`(local 프로필과 달리 배포 환경은 항상 env var 기반 datasource를 쓰는 `application-prod.yaml`을 활성화 — dev/prod 네임스페이스 구분은 Spring 프로필이 아니라 overlay의 namespace/replicas/설정값으로 한다), `AUTH_ISSUER_URI`/`AUTH_APP_CLIENT_ID`(Cognito, 비밀은 아니지만 환경별로 다른 User Pool을 쓸 수 있어 overlay의 `configmap-patch.yaml`에서 채움 — dev·prod 모두 실값이 채워져 있고, 현재는 prod CD 파이프라인 검증을 위해 양쪽이 같은 dev User Pool(`ap-northeast-2_y1mKz50El`)을 공용한다. 상용 전용 User Pool 준비 시 prod overlay만 교체 — `.harness/BACKLOG.md`). `AUTH_APP_CLIENT_ID`는 **backend-auth 저장소의 `COGNITO_BACKEND_CLIENT_ID`와 반드시 같은 값**이어야 한다 — 프론트엔드는 Cognito와 직접 로그인하지 않고 backend-auth가 Backend App Client로 발급한 Access Token이 그대로 Book Service에 오므로, 값이 어긋나면 서명·issuer·`token_use`를 통과한 뒤 `client_id` 불일치로 전량 401이 된다(CLIAR-188).
- Flyway 마이그레이션은 `spring.flyway.enabled: true`로 앱 기동 시 자동 실행되므로, `backend-record`(Python/Alembic)처럼 별도 `initContainer`로 마이그레이션을 분리하지 않는다.
- readiness/liveness probe는 `GET /health`(`com.chc.dpgb.health.HealthController`, 인증 불필요)를 대상으로 한다. ALB Ingress의 `alb.ingress.kubernetes.io/healthcheck-path`도 동일 경로를 쓴다. actuator health(`/actuator/health`)로 바꾸지 않았다 — 프로브는 커스텀 컨트롤러 그대로 두고 actuator는 메트릭 노출(dev, 관리 포트 8081)에만 쓴다.
- `argocd/application-dev.yaml`: `targetRevision: develop`, `path: k8s/overlays/dev`, `automated.prune+selfHeal` — dev는 완전 자동 배포(같은 클러스터 `kubernetes.default.svc`). `argocd/application-prod.yaml`은 `targetRevision: main`, `path: k8s/overlays/prod`로 활성화되어 있으며, dev와 달리 원격 `dpyb-prod` 클러스터(`destination.name: dpyb-prod`, 사전에 `argocd cluster add --name dpyb-prod` 등록 필요)로 배포한다.
- prod 네트워크: `dpyb-prod` VPC(`vpc-0e6d4633d86f40838`)의 private 서브넷 4개는 아웃바운드를 NAT Gateway(`nat-0c14a04ce3a253648`, `public2-ap-northeast-2b`)로 나간다 — 2026-08-30 이전에는 NAT가 없어 외부 API(알라딘) 호출과 Docker Hub pull이 모두 불가능했다. NAT는 단일 AZ이고 4개 라우팅 테이블 모두 이 하나를 가리킨다(`.harness/BACKLOG.md`에 AZ별 이중화 항목). VPC 엔드포인트는 S3(Gateway)·ecr.api·ecr.dkr 3종이며, ECR 이미지 pull은 NAT가 아니라 이 엔드포인트를 탄다. `dpyb-dev` VPC(`vpc-0093ef1d89d6bfd57`)는 별도 VPC이고 자체 NAT를 갖는다 — 두 VPC 모두 CIDR이 `10.0.0.0/16`이라 피어링은 불가능하다.
- DB 런타임: dev·prod 모두 Aurora PostgreSQL 17.7. prod 클러스터(`dpyb-prod`)는 2026-08-30에 provisioned `db.r7g.large` 2대(writer+reader) + I/O-Optimized에서 **Serverless v2 writer 1대(0.5~8 ACU) + Standard 스토리지**로 전환했다(dev는 이전부터 `db.serverless`). reader가 없어 페일오버가 느려지는 것은 런칭 전까지 감수하는 상태다(`.harness/BACKLOG.md`).
- ALB IngressClass(`alb`)는 `backend-auth` 레포에서 클러스터 전역으로 이미 구성되어 있고 여러 서비스가 공유한다 — 이 저장소에서 별도로 만들지 않는다.
- 배포용 `Dockerfile`은 2-스테이지다. 빌드 스테이지는 `FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk`로 러너 네이티브 아키텍처에 고정해 Gradle을 한 번만 실행하고(산출물 jar이 아키텍처 중립이므로 가능), 런타임 스테이지(`eclipse-temurin:21-jre`)만 타깃 아키텍처를 따라간다 — 멀티아키 빌드에 QEMU 에뮬레이션 비용이 들지 않는 이유다.
- 배포용 `Dockerfile`은 root로 실행되고(별도 `USER` 없음) non-root 사용자를 두지 않는다 — `k8s/base/deployment.yaml`의 `securityContext`도 이에 맞춰 `runAsNonRoot`/`readOnlyRootFilesystem`을 강제하지 않는다(CI/CD 구축 시점에 사용자가 확인, Dockerfile 자체는 변경하지 않기로 결정).

## Git

- 원격: `origin` = `https://github.com/dont-paw-get/backend-book.git`
- 브랜치: `main`(릴리스), `develop`(통합), `{티켓번호}-{설명}`(작업)
- 커밋 컨벤션: 저장소 루트 `README.md` 참조
