# ADR-0010: `genre`/`readingStatus` 재도입 (ADR-0003 genre 부분·ADR-0005 전체 반전)

- 상태: Accepted
- 일자: 2026-08-25

## 배경

사용자가 `shelf`/`library_book`/`scrap`/`librarian` 전체를 아우르는 확정 DB 스키마(SQL)를 제공했다. 이 스키마는 `library_book`에 `genre`(`genre_type` enum, 16종)와 `reading_status`(`book_reading_status` enum: `PLANNED`/`READING`/`COMPLETED`)를 다시 포함하고 있다.

- `genre`는 ADR-0003이 "알라딘 API가 언어 정보를 전혀 제공하지 않고 담당 기능표에도 없다"는 이유로 `moodTags`/`language`와 함께 제거했던 필드다. 이번 확정 스키마는 `genre`만 되살렸고 `moodTags`/`language` 제거는 그대로 유지한다 — ADR-0003의 나머지 결정(알라딘 단일 소스화, OCR/AI 삭제, 스크랩/사서 신설)은 반전하지 않는다.
- `readingStatus`는 ADR-0005가 "진도율(`progress`)만으로 충분하다"는 이유로 완전히 제거했던 필드다. 이번 확정 스키마는 이름은 비슷하지만 값 구성이 다른(`PLANNED`/`READING`/`COMPLETED`, 구버전은 `NOT_STARTED`/`READING`/`COMPLETED`) 필드로 다시 도입한다.

## 결정

1. `LibraryBook`에 `genre`(선택, 생략 시 `NONE`)와 `readingStatus`(선택, 생략 시 `PLANNED`)를 다시 추가한다.
2. 두 필드는 `progress`와 자동으로 연동되지 않는 독립 필드다 — 서버가 진도율로부터 상태를 추론하지 않고, 클라이언트가 명시적으로 설정한 값을 그대로 저장·반환한다.
3. `createLibraryBook`에서는 둘 다 선택 필드다. `updateLibraryBook`은 ADR-0006의 "항상 전체 필드 포함" 방식을 그대로 따르므로 `genre`/`readingStatus`도 요청에 항상 포함해야 하며, `null`을 허용하지 않는다(값이 없으면 `NONE`/`PLANNED`를 명시적으로 보낸다).
4. `totalPages`도 이번 스키마 변경(`total_pages` nullable화)에 맞춰 선택 필드로 전환한다 — 알라딘 API가 페이지 수를 제공하지 않는 도서가 많아 등록 시점에 모를 수 있다는 점을 그대로 반영한 것이다. `totalPages`가 없으면 `progress`는 `null`이다.

## 결과

- `docs/api/openapi.yaml`(v0.8.0)에 `Genre`/`ReadingStatus` enum 스키마를 추가하고, `CreateLibraryBookRequest`/`UpdateLibraryBookRequest`/`CreateLibraryBookResponse`/`UpdateLibraryBookResponse`/`LibraryBookSummary`/`LibraryBookDetailResponse`/`UpdateReadingProgressRequest`/`UpdateReadingProgressResponse`에 반영했다.
- `com.chc.dpgb.library.domain.LibraryBook`에 `Genre genre`/`ReadingStatus readingStatus` 필드와 도메인 검증(둘 다 `updateMetadata`에서는 `null` 불허, `register`에서는 생략 시 기본값)을 추가했다. `totalPages`를 `Integer`로 바꾸고 `progress()`가 `totalPages`가 없으면 `null`을 반환하도록 확장했다.
- Postgres 네이티브 enum(`genre_type`, `book_reading_status`)은 Hibernate `@Enumerated(STRING) + @JdbcTypeCode(SqlTypes.NAMED_ENUM)`으로 매핑한다 — 이 저장소 최초의 네이티브 enum 컬럼 사용 사례.
- `.harness/DOMAIN.md`의 LibraryBook aggregate 절에 두 필드와 `totalPages` nullable 규칙을 반영한다(문서 갱신은 별도 작업으로 처리).
