# ADR-0013: nullable 필드는 생략이 아니라 null로 전송 + 요청 검증을 Bean Validation으로 강제

- 상태: Accepted
- 일자: 2026-08-31

## 배경

`openapi.yaml`을 DB 스키마(Flyway V1~V9)·JPA 엔티티·DTO·컨트롤러와 대조한 결과 세 갈래 불일치가 드러났다.

1. **null 직렬화 불일치.** 명세는 nullable 컬럼(`isbn`, `publisher`, `cover_url`, `page_number`, `total_pages`, `published_date`)을 non-nullable 타입 + "없으면 생략"으로 기술했다. 그러나 응답 DTO가 전부 Java record이고 `spring.jackson.default-property-inclusion` 설정이 없어, Jackson은 값이 없어도 **키를 달고 `null`을 실어 보낸다**. 같은 파일 안에서도 규칙이 갈렸다 — `ScrapDetailResponse.pageNumber`는 `[integer, "null"]`인데 `ScrapSummary.pageNumber`는 `integer`, `LibraryBookDetailResponse.totalPages`는 `[integer, "null"]`인데 바로 옆 `isbn`/`publisher`/`coverUrl`은 아니었다. 의도적 설계가 아니라 누락이다.
2. **요청 검증 부재.** `spring-boot-starter-validation` 의존성이 없고 `@Valid`/`@NotNull`/`@Size`/`@Pattern` 사용이 0건이었다. 명세의 `required`/`minLength`/`maxLength`/`pattern`/`minimum`이 런타임에 하나도 강제되지 않았다.
3. **파생 오작동.** 검증이 없어 `title: null`인 생성 요청이 그대로 영속화 단계까지 내려가고, `LibraryBookService.createLibraryBook`의 `catch (DataIntegrityViolationException) → BookAlreadyRegisteredException`이 이를 삼켜 **400이어야 할 요청이 409 "이미 등록된 도서"로 응답**될 수 있었다.

## 결정

1. **nullable 표기는 명세를 구현에 맞춘다.** Jackson의 null 포함 동작(`ALWAYS`)을 유지하고, `openapi.yaml`의 해당 필드를 `[X, "null"]`로 고친다. `default-property-inclusion: non_null`로 구현을 명세에 맞추는 선택지도 있었으나, 프론트엔드가 이미 `null`을 받는 전제로 동작 중이라 런타임 계약을 바꾸는 쪽이 위험하다. **키의 존재 여부가 아니라 값의 null 여부로 판정하는 것이 이 API의 규칙이다.**
2. **응답 스키마의 `required`는 모든 property를 포함한다.** record DTO는 모든 컴포넌트를 항상 직렬화하므로, 응답에서 "없을 수 있는 키"는 존재하지 않는다. `required`에서 빠져 있던 것은 nullable 표현을 `required` 제외로 잘못 대체한 흔적이다.
3. **`$ref`만으로 nullable을 표현할 수 없으므로 `oneOf`를 쓴다.** `BookSearchResponse.libraryBook`/`book`은 `oneOf: [$ref, {type: "null"}]`로 바꾼다. `$ref` 옆에 형제 키를 두면 JSON Schema 2020-12에서 무시되지 않지만 `type`을 덧붙일 수는 없다.
4. **요청 검증은 구현을 명세에 맞춘다.** `spring-boot-starter-validation`을 도입하고 요청 DTO 11개에 명세와 1:1 대응하는 제약을 선언한다.
5. **검증 실패의 에러 코드는 기존 endpoint별 코드를 유지한다.** `MethodArgumentNotValidException` 하나로는 `INVALID_BOOK_DATA`/`INVALID_SCRAP_DATA`/`INVALID_SHELF_DATA`/`INVALID_LIBRARIAN_DATA`를 구분할 수 없으므로, **요청 DTO 타입 → 도메인 예외 매핑을 `GlobalExceptionHandler` 한 곳**에 둔다. 공통 코드(`INVALID_REQUEST`) 신설은 계약이 단순해지는 대신 프론트엔드의 기존 코드 분기를 깨뜨려 채택하지 않았다.
6. **요청 스키마에서 `additionalProperties: false`를 제거한다.** Spring Boot 기본값이 `fail-on-unknown-properties=false`라 모르는 필드는 조용히 무시된다. Bean Validation으로는 이를 막을 수 없고, 설정을 켜서 구현을 명세에 맞추면 프론트엔드가 여분 필드를 하나라도 보내는 순간 전 요청이 400이 되는 위험이 있다. **응답 스키마의 `additionalProperties: false`는 사실이므로 유지한다** — 서버는 정의된 필드만 보낸다.
7. **`sentence`/`memo`의 `maxLength: 2000`은 명세를 유지하고 애플리케이션이 강제한다.** V8에서 컬럼이 `VARCHAR(2000)` → `TEXT`로 바뀌어 DB는 더 이상 길이를 제한하지 않는다. 스키마를 되돌리는 대신 `@Size(max = 2000)`로 계약을 지킨다.
8. **`publishedDate`를 `LibraryBookDetailResponse`에 추가한다.** 생성·수정 요청으로 저장은 되는데 어떤 응답도 돌려주지 않아 왕복이 닫히지 않았다. 목록(`LibraryBookSummary`)에는 추가하지 않는다 — 목록 카드에 필요한 값이 아니다.

## 결과

- `docs/api/openapi.yaml`: 응답 nullable 표기 정정(`LibraryBookSummary`, `LibraryBookDetailResponse`, `UpdateLibraryBookResponse`, `ScrapSummary`, `ExternalBook`, `BookSearchResponse`), 응답 스키마 `required` 완성, 요청 스키마 11개의 `additionalProperties: false` 제거, `LibraryBookDetailResponse.publishedDate` 추가.
- `build.gradle`: `spring-boot-starter-validation` 추가.
- 요청 DTO 11개에 Bean Validation 제약, 컨트롤러 `@RequestBody` 11곳에 `@Valid`.
- `GlobalExceptionHandler`: `MethodArgumentNotValidException` → 요청 DTO별 도메인 예외 매핑.
- `LibraryBookService.createLibraryBook`: `DataIntegrityViolationException` 포괄 catch를 ISBN 중복으로 한정.
- `LibraryBookDetailResponse`: `publishedDate` 필드 추가.
