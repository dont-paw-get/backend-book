# DOMAIN (현재 업무 규칙)

이 문서는 지금 시점의 업무 규칙(aggregate가 소유하는 개념, 불변식, 상태 전이, 중복 판정)만 담는다. API wire 표현은 `docs/api/openapi.yaml`, 결정 이유는 `DECISIONS.md`, 구현 진행 상황은 `STATE.md`를 본다.

이 저장소(Book Service)가 다루는 범위는 `docs/api/openapi.yaml`에 명세된 것과 동일하다: 알라딘 API 기반 외부 도서 검색, 개인 서재(LibraryBook) 관리, 책장(Shelf) 관리, 독서 진도, 문장 스크랩(Scrap), 동물 사서(Librarian). 표지 이미지에서 텍스트를 추출하는 OCR과 AI 기반 도서·무드 분석, 오늘의 기분 추천은 다른 MSA 컴포넌트 담당이라 이 문서의 범위 밖이다.

**CLIAR-43에서 스크랩(Scrap)을 범위에 포함하도록 재조정했다.** 기존에는 "문장 OCR·감상·비밀 메모는 범위 밖"으로 판단했었지만(ADR-0002), 사용자가 담당 범위를 재확인하면서 문장 저장·페이지 번호·감상 및 메모 CRUD가 이 API Server 담당임을 확정했다. 문장을 이미지에서 추출하는 OCR 자체와 스크랩 이미지 업로드(오브젝트 스토리지 필요)는 여전히 범위 밖이며, 이 서비스는 이미 텍스트로 확정된 문장만 받아 저장한다. 사유는 `docs/api/decisions/0003-scope-narrowing-and-new-resources.md` 참조.

**같은 작업에서 `language` 필드도 완전히 제거했다.** ADR-0002가 도입했던 "사용자가 선택 입력, 생략 시 서버가 `ko`로 채운다"는 규칙은 더 이상 유효하지 않다 — 알라딘 API가 언어 정보를 전혀 제공하지 않고, 담당 기능표에도 언어 관리 기능이 없어 LibraryBook aggregate에서 제거했다.

## 외부 도서 검색 → LibraryBook 생성 경계

- 알라딘 API 검색 결과(또는 검색 결과가 없어 사용자가 직접 입력한 값 — 폴백)는 사용자가 서재 등록을 확정하기 전까지는 후보 데이터다.
- 사용자가 최종 확인해 등록을 요청하는 시점이 LibraryBook 생성 경계다.
- 사용자가 확정한 값은 이후 외부 도서 API 재조회로 자동 덮어쓰지 않는다.
- 알라딘 응답의 `author`는 "세네카 (지은이), 하와이 대저택 (편역)"처럼 역할 라벨이 붙은 결합 문자열이다. 검색 응답을 만드는 시점에 서버가 "(지은이)"/"(옮긴이)"/"(편역)" 등 역할 라벨을 제거하고 이름만 남긴 뒤 여럿이면 쉼표로 구분해 반환한다 — 이후 서재 저자 필터·정렬은 이 정리된 이름을 기준으로 한다.
- `totalPages`는 알라딘 API가 대부분의 도서에서 제공하지 않는다. 검색 결과에 없으면 사용자가 서재 등록 시 직접 입력해야 하는 것이 예외가 아니라 일반적인 경로다.

## LibraryBook aggregate

소유하는 개념:

- 소유자 식별자 `memberId`(요청 body가 아니라 인증 principal에서 얻는다)
- 서버 발급 식별자 `bookId`
- 소속 책장 `shelfId`(Shelf aggregate 참조, 필수) — 등록 시 생략하면 사용자의 기본 책장에 배치
- 책장 내 순서 `shelfRank` (LexoRank 방식의 불투명한 문자열 순서 키, 등록 시점에 서버가 그 책장의 맨 뒤 순서로 부여하고 등록 응답에 포함한다)
- 확인된 책 메타데이터: `title`, `author`, `isbn`, `publisher`, `publishedDate`, `coverUrl`
- `totalPages`, `currentPage`
- 생성·수정 시각

모든 조회와 변경은 aggregate 소유자(`memberId`) 기준으로 수행한다.

`coverUrl`은 등록(`createLibraryBook`)과 수정(`updateLibraryBook`) 두 경로로만 갱신된다. 값은 문자열(URL)이며, 알라딘 검색 결과의 표지 URL을 그대로 쓰거나 사용자가 다른 이미지 URL을 직접 입력한다. 사용자가 직접 촬영한 이미지 파일을 업로드해 저장(S3 등)하는 기능은 이 저장소 범위 밖이다 — 필요해지면 파일 저장을 담당할 별도 컴포넌트/서비스와 함께 재설계한다.

### `updateLibraryBook` 수정 방식 (ADR-0006)

`updateLibraryBook`은 부분 수정이 아니라 `title`/`author`/`isbn`/`publisher`/`publishedDate`/`coverUrl`/`totalPages` 7개 필드를 **항상 모두 포함**해야 하는 계약이다(`Scrap.updateScrap`과 동일한 방식). `isbn`/`publisher`/`publishedDate`/`coverUrl`는 nullable 필드라 `null`을 보내면 그 값을 삭제하고, 값을 보내면 교체한다. `title`/`author`/`totalPages`는 aggregate의 필수 불변값이라 `null`을 허용하지 않는다.

### `shelfRank` (책장 내 순서, ADR-0004, ADR-0008로 범위 재조정)

- `shelfRank`는 LexoRank 방식의 불투명한 문자열 순서 키다. 오름차순 문자열(사전식) 비교가 곧 그 책장 안에서의 진열 순서이며, 값 자체는 사용자·클라이언트에게 아무 의미를 갖지 않는다.
- **범위는 책장별(`shelfId`)로 유일해야 한다**(ADR-0008로 사용자 전역에서 책장별로 좁혔다) — 동시 등록·재정렬에도 중복이 생기지 않도록 DB unique 제약(`shelfId`, `shelfRank`)을 함께 사용한다. 서로 다른 책장의 책은 `shelfRank` 값이 같아도 무방하다.
- 등록(`createLibraryBook`) 시점에 서버가 그 책장의 현재 마지막 `shelfRank`보다 뒤에 오는 값을 자동 부여한다(맨 뒤에 추가). 책장이 비어 있으면 기본 시작 값을 부여한다.
- 순서 변경은 오직 전용 API(`reorderLibraryBook`, `PATCH /api/v1/library/books/{bookId}/order`)로만 한다 — `updateLibraryBook`(PATCH 본문)으로는 `shelfRank`를 바꿀 수 없다. 클라이언트는 "이 책을 어떤 책의 앞/뒤로 옮겨줘"라고만 요청하고(`beforeBookId`/`afterBookId` 중 정확히 하나), 서버가 그 두 이웃 사이에 들어갈 새 `shelfRank`를 계산해 저장한다.
- 재정렬 대상(`beforeBookId`/`afterBookId`)은 같은 책장에 속해야 하고, 옮기려는 책 자신을 지정할 수 없다 — 위반 시 400(`INVALID_REORDER_TARGET`). 다른 책장으로 옮기려면 `reorderLibraryBook`이 아니라 `moveLibraryBookToShelf`를 쓴다.
- 두 이웃 `shelfRank` 사이에 더 끼워넣을 문자열 여유가 없어지면(반복 삽입으로 키 공간이 소진된 극단적 경우) 서버가 해당 책장 전체의 `shelfRank`를 넓은 간격으로 재계산(rebalance)한다. 이 재계산은 클라이언트에 노출되는 API가 아니라 서버 내부 유지보수 동작이다.
- `GET /api/v1/library/books`(전체 책장 합산, `shelfId` 필터 선택)와 `GET /api/v1/library/shelves/{shelfId}/books`(특정 책장)의 `sortBy=SHELF_ORDER`(기본값)는 `shelfRank` 오름차순(`sortOrder` 기본값 `ASC`)이 자연스러운 진열 순서다. `LibraryBookSummary`에도 `shelfId`/`shelfRank`가 함께 노출된다.
- 책장을 옮기면(`moveLibraryBookToShelf`) 대상 책장의 맨 뒤에 새 `shelfRank`가 부여된다 — 이전 책장에서의 순서는 유지되지 않는다.

## Shelf aggregate (ADR-0008)

소유하는 개념:

- 소유자 식별자 `memberId`(요청 body가 아니라 인증 principal에서 얻는다)
- 서버 발급 식별자 `shelfId`
- `name`(사용자가 정한 책장 이름)
- `isDefault`(그 사용자의 기본 책장 여부 — 서버 전용 플래그, 클라이언트가 지정·변경할 수 없다)
- 생성·수정 시각

불변식:

- 사용자마다 기본 책장(`isDefault=true`)이 정확히 1개 존재한다. 이 서비스는 회원가입/계정 생성 이벤트를 소유하지 않으므로, 별도 초기화 훅 없이 그 사용자의 책장 관련 동작(서재 책 등록, 책장 목록 조회 등)이 처음 필요해지는 시점에 서버가 없으면 생성한다(get-or-create) — 항상 존재하는 것처럼 보이되 실제 생성은 지연된다.
- 기본 책장은 삭제할 수 없다 — 시도하면 400(`DEFAULT_SHELF_CANNOT_BE_DELETED`).
- 기본 책장의 이름은 다른 책장과 마찬가지로 자유롭게 바꿀 수 있다. "기본"이라는 성질은 `isDefault` 플래그로만 구분되고 이름과는 무관하다.
- 책장 이름 중복은 서버가 막지 않는다 — 같은 사용자가 같은 이름의 책장을 여러 개 가질 수 있다(LibraryBook의 제목·저자 중복을 사용자 자율에 맡긴 ADR-0007과 같은 기조).
- 책장을 삭제하면 그 안에 있던 모든 `LibraryBook`이 그 사용자의 기본 책장 맨 뒤로 이동한다(`shelfRank` 재계산 포함).
- `LibraryBook`은 항상 정확히 하나의 책장에 속한다(`shelfId` 필수) — 등록 시 생략하면 기본 책장에 배치된다. 책장 간 이동은 `moveLibraryBookToShelf`로만 한다.
- 모든 조회와 변경은 aggregate 소유자(`memberId`) 기준으로 수행한다 — 다른 사용자의 책장에 접근하면 403(`SHELF_ACCESS_DENIED`).

## Scrap aggregate

소유하는 개념:

- 소유자: 독립적으로 memberId를 갖지 않고 LibraryBook을 통해 귀속되는 하위 리소스다. 접근 권한 검증은 스크랩이 속한 LibraryBook의 소유자(`memberId`) 기준으로 한다.
- `scrapId`, `bookId`, `sentence`(필수), `pageNumber`(선택), `memo`(선택), 생성·수정 시각

불변식:

- `sentence`는 항상 값이 있어야 한다. 빈 문자열이나 삭제는 허용하지 않는다 — 문장 자체를 없애려면 스크랩을 삭제한다.
- `pageNumber`·`memo`는 선택 값이며 사용자가 추가·수정·삭제할 수 있다.
- 스크랩 수정은 `sentence`/`pageNumber`/`memo` 세 필드를 항상 모두 포함해야 하는 계약이다(부분 생략 불가). `pageNumber`·`memo`에 `null`을 보내면 그 값을 삭제하고, 값을 보내면 교체한다. `sentence`는 `null`을 허용하지 않는다.
- 스크랩에 이미지를 연결하는 기능(사진 촬영/업로드)은 이 저장소 범위 밖이다 — 파일 저장(S3 등)이 필요해 LibraryBook 표지와 같은 이유로 제외했다.

## Librarian / 대표 사서

소유하는 개념:

- `Librarian`(사서 마스터 데이터, 시스템이 관리): `librarianId`, `name`, `type`(고양이/새/곰/달팽이 등 — 종류가 늘어날 수 있어 고정 enum이 아니라 서버가 관리하는 문자열), `imageUrl`, `evolutionStage`
- 회원별 대표 사서 선택: `memberId` → `librarianId`, 선택 시각

불변식:

- 회원은 대표 사서를 최대 1개만 가질 수 있고, 언제든 다른 사서로 변경할 수 있다 — 새 선택이 이전 선택을 덮어쓴다.
- 존재하지 않는 `librarianId`는 선택할 수 없다(404).
- 아직 대표 사서를 선택하지 않은 회원의 조회는 404로 응답한다 — 선택을 강제로 기본값 지정하지 않고 미선택 상태를 명시적으로 구분한다.

## 페이지와 진도율 (ADR-0005 반영)

- `totalPages > 0`
- `0 <= currentPage <= totalPages`
- 진도율은 서버가 `currentPage / totalPages * 100`으로 계산한다.
- 전체 페이지를 기존 현재 페이지보다 작게 줄일 수 없다.
- 이전 페이지로의 이동은 사용자의 위치 정정으로 허용한다.
- `ReadingStatus`(`NOT_STARTED`/`READING`/`COMPLETED`)는 ADR-0005로 제거했다 — 진행 상태는 `progress`(진도율)만으로 표현하고, 별도 상태 필드나 필터는 두지 않는다.

## 중복 (ADR-0007 반영)

- 중복은 사용자별로 판정한다. 다른 사용자는 같은 책을 각각 등록할 수 있다.
- ISBN이 있으면 사용자별 ISBN 기준으로 중복을 판정한다 — 같은 사용자가 같은 ISBN을 두 번 등록할 수 없다.
- ISBN이 없으면 중복을 판정하지 않는다 — 같은 사용자가 제목·저자가 같은 책을 여러 번 등록해도 막지 않는다. 다른 책이어도 제목·저자가 우연히 같을 수 있어, 그 판단은 서버가 강제하지 않고 사용자 자율에 맡긴다.
- 동시 등록에서도 ISBN 중복이 생기지 않도록 저장소 제약(DB unique constraint)을 함께 사용한다.

## 결정된 사항 (ADR-0002 반영)

- `coverUrl`은 등록 시점뿐 아니라 이후 수정(PATCH)으로도 변경할 수 있다.
- `shelfRank`는 등록 응답 시점부터 클라이언트에 노출된다.

## 결정된 사항 (ADR-0004 반영)

- `bookNumber`(정수 순번)를 `shelfRank`(LexoRank 문자열 키)로 대체했다. 재정렬 시 서버가 이동한 책 하나만 갱신하면 되고, 사용자별 유일성은 DB unique 제약으로 보장한다.
- 재정렬은 `updateLibraryBook`이 아니라 전용 `reorderLibraryBook` API로 분리했다 — 클라이언트가 유효하지 않은 순서 키를 직접 계산해 보낼 위험을 없앤다.
- 목록 조회(`getLibraryBooks`)의 기본 정렬을 `SHELF_ORDER` 오름차순으로 바꿔, 사용자가 재배열한 순서가 실제 목록에 반영되게 했다.

## 결정된 사항 (ADR-0005 반영)

- `ReadingStatus`(`NOT_STARTED`/`READING`/`COMPLETED`)를 aggregate·계약에서 완전히 제거했다. `getLibraryBooks`의 `readingStatus` 필터, `UpdateReadingProgressResponse.readingStatus`가 함께 삭제됐다.
- 진행 상태는 `progress`(진도율) 하나로만 표현한다 — 별도 상태 열거값·필터는 두지 않는다.

## 결정된 사항 (ADR-0006 반영)

- `updateLibraryBook`을 부분 수정에서 `Scrap.updateScrap`과 동일한 "항상 전체 필드 포함, nullable 필드는 `null`=삭제" 방식으로 통일했다.
- 이 저장소의 두 PATCH 리소스(LibraryBook, Scrap)가 서로 다른 수정 방식을 쓰지 않도록 규칙을 하나로 맞췄다.

## 결정된 사항 (ADR-0007 반영)

- ISBN이 없는 책에 대한 "정규화된 제목+저자" 중복 판정을 완전히 제거했다 — 서로 다른 책이라도 제목·저자가 우연히 같을 수 있어, 그 판단을 서버가 강제하지 않고 사용자 자율에 맡긴다.
- ISBN이 있는 책의 사용자별 유일성 판정만 남는다.

## 결정된 사항 (ADR-0008 반영)

- 책장(Shelf) aggregate를 신설했다 — 사용자는 여러 책장을 만들고 책을 책장 간에 옮길 수 있다.
- `shelfRank`의 유일성 범위를 사용자 전역(`memberId`)에서 책장별(`shelfId`)로 좁혔다 — "책장별 책 목록 조회"가 그 책장 안에서의 진열 순서를 보여줘야 자연스럽기 때문. `reorderLibraryBook`은 같은 책장 내에서만 가능하고, 책장 간 이동은 별도 API(`moveLibraryBookToShelf`)로 분리했다.
- 기본 책장은 계정 생성 이벤트 없이 필요 시점에 서버가 자동 생성(get-or-create)한다. 삭제는 금지하지만 이름 변경은 허용한다.
- 책장 이름 중복은 서버가 강제하지 않는다.

## 미결정 도메인

- 표지 색상(`coverColor`)의 생성 주체와 허용 값 — 저장 필드가 정의되면 별도로 다시 설계 (ADR-0002에서 관련 필터는 우선 제거)
- 스크랩 목록 조회의 정렬 기준(현재는 서버 기본 정렬만 있고 클라이언트가 선택할 수 있는 sortBy가 없음) — 필요해지면 Library 목록과 같은 방식으로 추가

결정된 규칙이 API에 노출되면 `docs/api/openapi.yaml`과 계약 테스트도 함께 갱신한다.
