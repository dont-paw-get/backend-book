# ADR-0003: 담당 범위 재확정 — 장르/무드 제거, 알라딘 단일 소스화, 스크랩·동물 사서 추가, 이미지 파일 업로드 기능 제외

- 상태: Accepted
- 일자: 2026-08-20

## 배경

CLIAR-43에서 이 저장소(Book Service)가 실제로 담당하는 기능을 "단순 데이터베이스 CRUD로 해결 가능한 부분"으로 사용자와 함께 재확인했다. 그 결과 기존 계약과 다음 지점에서 차이가 발견됐다.

1. 장르(`genre`)와 무드(`moodTags`)는 더 이상 이 서비스가 추출·저장·필터링하지 않기로 했다. AI 기반 자동 생성(Bedrock)도, 외부 도서 API의 카테고리 정보를 장르로 매핑하는 것도 하지 않는다.
2. 표지 OCR(`POST /api/v1/books/ocr`)과 AI 도서 분석(`POST /api/v1/books/ai-analyze`)은 이미지 인식·LLM 분석이 필요한 기능으로, 사용자가 제시한 담당 기능표에 포함되지 않았다 — 다른 MSA 컴포넌트가 담당하고, 이 API Server는 이미 인식된 제목/저자만 받아 외부 도서 API 조회를 수행한다.
3. 외부 도서 검색은 여러 소스를 가정한 계약(`ExternalBook.source`, 예시 `NATIONAL_LIBRARY`)이었지만, 실제로는 알라딘 API 하나만 사용한다.
4. 사용자가 제시한 담당 기능표에는 문장 스크랩 CRUD, 동물 사서(대표 사서 선택·마스터 정보), 책 표지 커스터마이징(사용자 업로드)이 포함되어 있었다 — 이 중 스크랩은 `docs/api/decisions/0002-library-book-schema-fixes.md`가 "문장 OCR·감상·비밀 메모는 다른 MSA 컴포넌트 담당이라 범위 밖"이라고 명시했던 것과 정면으로 상충한다. 사용자에게 이 상충을 직접 확인한 결과, **스크랩 CRUD를 이 저장소 범위로 재편입**하기로 확정했다. 문장을 이미지에서 추출하는 OCR 자체(텍스트 인식)는 여전히 범위 밖이며, 이 서비스는 이미 텍스트로 확정된 문장만 받아 저장·조회·수정·삭제한다.
5. `language`(ISO 639-1)는 ADR-0002가 "사용자가 선택 입력, 생략 시 서버가 `ko`로 채운다"로 도입했던 필드다. 알라딘 API가 언어 정보를 전혀 제공하지 않고, 사용자가 제시한 담당 기능표에도 언어 관리 기능이 없어, 서재 등록·조회·수정 전 구간에서 제거하기로 확정했다.
6. 서재 책 표지 이미지 교체(`PUT /api/v1/library/books/{bookId}/cover`)와 스크랩 이미지 교체(`PUT /api/v1/library/scraps/{scrapId}/image`)는 처음엔 신규 리소스로 추가했으나, 두 endpoint 모두 사용자가 업로드한 이미지 파일을 저장할 오브젝트 스토리지(S3 등)가 필요하다는 게 뒤늦게 드러났다. 이 저장소가 담당하는 범위는 "단순 데이터베이스 CRUD로 해결 가능한 부분"으로 이미 못박았고, 파일 저장소 연동은 여기 해당하지 않는다 — 사용자에게 확인받아 두 endpoint를 계약에서 제거했다.

## 결정

1. `genre`, `moodTags` 필드와 관련 필터 파라미터(`moodTags` 쿼리 파라미터)를 모든 스키마에서 제거한다. `MoodTags` 컴포넌트 스키마도 삭제한다.
2. `POST /api/v1/books/ocr`, `POST /api/v1/books/ai-analyze` 엔드포인트와 관련 스키마(`BookOcrResponse`, `AiBookAnalysisRequest`, `AiBookAnalysisResponse`)를 계약에서 완전히 삭제한다.
3. `GET /api/v1/books/search`의 `ExternalBook` 스키마를 알라딘 API가 실제로 제공하는 필드(`title`, `author`, `isbn`, `publisher`, `publishedDate`, `totalPages`, `coverUrl`)로 한정하고, 다중 소스 구분용이었던 `source`, 목록에 없던 `kdc`, 미사용이던 `externalBookId`를 제거한다. 외부 API 오류 응답 코드를 `BOOK_API_ERROR`에서 `ALADIN_API_ERROR`로 바꾼다.
4. `GET /api/v1/library/books`의 `sortBy`에 `PROGRESS`를 추가해 독서 진행률 기준 정렬을 지원한다(필터는 `author`/`readingStatus`만 유지).
5. 스크랩(Scrap) 리소스를 신설한다: 책별 생성·목록·상세 조회, 문장/페이지 번호/메모 수정, 삭제(이미지 교체는 6번 참조로 제외). 수정(`PATCH`)은 `sentence`/`pageNumber`/`memo`를 항상 모두 포함해야 하는 계약으로 설계해, `pageNumber`·`memo`에 `null`을 보내면 삭제, 값을 보내면 교체하는 명확한 의미를 갖게 한다.
6. 동물 사서(Librarian) 리소스를 신설한다: 사서 마스터 목록 조회, 내 대표 사서 조회, 대표 사서 선택/변경. `type`은 종류가 늘어날 수 있어 고정 enum이 아니라 서버가 관리하는 문자열로 둔다.
7. 서재 책의 실제 표지 우선 적용은 별도 endpoint 없이, 클라이언트가 검색 결과의 `coverUrl`(문자열 URL)을 등록/수정 요청에 그대로 전달하는 흐름으로만 지원한다. 사용자가 촬영/선택한 이미지 파일을 서버가 저장(S3 등)해 표지로 쓰는 기능은 지원하지 않는다.
8. (폐기됨 — 6번 참조) 표지·스크랩 이미지 업로드 제약(JPEG/PNG/WEBP, 최대 10MB)은 두 endpoint가 제거되면서 더 이상 이 계약에 없다. 파일 업로드 기능을 다시 도입할 때 재검토한다.
9. `language` 필드를 `CreateLibraryBookRequest`, `UpdateLibraryBookRequest`, `UpdateLibraryBookResponse`, `LibraryBookSummary`, `LibraryBookDetailResponse`와 `GET /api/v1/library/books`의 필터 파라미터에서 전부 제거한다. ADR-0002의 "생략 시 서버가 `ko`로 채운다"는 규칙은 이 ADR로 폐기된다.
10. 스크랩의 `imageUrl` 필드(`ScrapDetailResponse`)와 `CoverImageResponse`/`ScrapImageResponse` 스키마, `InvalidCoverImageFile`/`InvalidScrapImageFile` 오류 응답을 계약에서 제거한다 — 이미지를 설정할 방법이 없어진 필드를 남겨두지 않는다.

## 결과

- `docs/api/decisions/0002-library-book-schema-fixes.md`의 "스크랩은 범위 밖", "`language` 생성 주체" 결정은 이 ADR로 대체된다. 스크랩 관련 문장·페이지 번호·감상 및 메모는 이제 이 저장소가 소유하고, `language`는 계약에서 완전히 사라진다.
- 이미지 파일 업로드(표지 교체, 스크랩 이미지 교체)는 오브젝트 스토리지 연동이 필요해 "단순 DB CRUD" 범위를 벗어난다고 판단해 계약에서 제외했다 — 필요해지면 파일 저장을 담당할 별도 컴포넌트/서비스와 함께 다시 설계한다(`.harness/BACKLOG.md` 후보).
- API 계약이 실제 담당 범위(단순 CRUD)와 다른 MSA 컴포넌트의 책임(이미지 인식, AI 분석, 파일 저장, 오늘의 기분 추천)을 명확히 분리한다.
- `.harness/DOMAIN.md`에 Scrap·Librarian aggregate 규칙을 새로 추가하고, LibraryBook aggregate에서 `genre`·`moodTags`·`language`를 제거했다.
- 도메인 구현(`LibraryBook`, `Scrap`, `Librarian`, `MemberLibrarianSelection` 등)과 계약 테스트는 아직 시작하지 않았다 — `.harness/PLAN.md` 참조.

## 이후 반전됨

- **결정 1(`genre` 제거)만 ADR-0010으로 반전됐다** — `library_book`에 `genre`가 재도입됐다. `moodTags`/`language` 제거, OCR·AI 엔드포인트 삭제, 알라딘 단일 소스화 등 나머지 결정은 그대로 유효하다.
- **결정 6(Librarian 리소스)은 ADR-0011로 전면 재설계됐다** — `type`은 더 이상 "서버가 관리하는 자유 문자열"이 아니라 고정 enum(`RUSSIAN_BLUE`/`SHOEBILL`)이고, "사서 마스터 목록 조회 + 대표 사서 선택"이 "타입 카탈로그 조회 + 회원별 사서 획득/개명/방출 + 대표 지정"으로 확장됐다.
