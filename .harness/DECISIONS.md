# DECISIONS (결정 이력, 최신이 위)

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
