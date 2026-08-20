# ADR-0004: `bookNumber`를 LexoRank 기반 `shelfRank`로 재설계

- 상태: Accepted
- 일자: 2026-08-20

## 배경

`bookNumber`는 "사용자 서재 내 순서"를 나타내려고 등록 시점에 서버가 부여하는 정수 필드였다(ADR-0002). 실제 구현을 시작하기 전 검토한 결과 다음 문제가 확인됐다.

1. 정수 순번 방식은 사용자가 책 한 권을 서재 중간으로 옮길 때마다 그 뒤에 있는 모든 책의 `bookNumber`를 다시 매겨야 한다. 서재 도서 수가 늘어날수록 재정렬 1회당 UPDATE 대상 행이 늘어나고, 동시에 여러 기기에서 재정렬하면 충돌 가능성도 커진다.
2. `.harness/DOMAIN.md`에 "`bookNumber`의 사용자별 유일성과 재정렬 규칙"이 미결정 상태로 남아 있었다 — 정수 방식을 그대로 구현에 들어가면 이 문제를 그대로 안고 가게 된다.
3. `UpdateLibraryBookRequest`가 `bookNumber`를 다른 도서 메타데이터 수정 필드와 함께 받고 있어, 클라이언트가 유효하지 않은(중복되거나 순서가 꼬이는) 값을 직접 계산해 보낼 위험이 있었다.
4. `GET /api/v1/library/books`의 `sortBy`(`TITLE`/`AUTHOR`/`CREATED_AT`/`PROGRESS`)와 `LibraryBookSummary` 응답 어디에도 `bookNumber`가 없어, 사용자가 재정렬한 순서가 실제 목록 조회 결과에 전혀 반영되지 않는 상태였다 — 필드만 존재하고 쓰이지 않는 값이었다.

## 결정

1. `bookNumber`(정수)를 `shelfRank`(LexoRank 방식의 불투명한 문자열 순서 키)로 대체한다. 정렬은 `shelfRank`의 오름차순 문자열(사전식) 비교로 정의한다. `CreateLibraryBookResponse`, `LibraryBookDetailResponse`, `LibraryBookSummary`, `UpdateLibraryBookResponse`에 반영한다.
2. `shelfRank`는 사용자(`memberId`)별로 유일해야 하며, DB unique 제약(`memberId`, `shelfRank`)으로 보장한다.
3. 등록(`createLibraryBook`) 시점에 서버가 그 사용자 서재의 현재 마지막 `shelfRank`보다 뒤에 오는 값을 자동 부여한다(맨 뒤에 추가). 클라이언트는 등록 요청에서 순서를 지정할 수 없다.
4. 순서 변경을 `updateLibraryBook`(PATCH 본문)에서 완전히 분리해 전용 endpoint `PATCH /api/v1/library/books/{bookId}/order`(`reorderLibraryBook`)를 신설한다. 요청은 `beforeBookId` 또는 `afterBookId` 중 정확히 하나만 받아 "이 책의 앞/뒤로 옮겨줘"라는 이웃 관계만 지정하고, 서버가 그 사이에 들어갈 `shelfRank` 값을 계산해 저장한다 — 클라이언트는 `shelfRank` 값 자체를 생성·계산하지 않는다.
5. 이웃으로 지정한 책(`beforeBookId`/`afterBookId`)이 같은 사용자 서재에 없거나 옮기려는 책 자신이면 400(`INVALID_REORDER_TARGET`)으로 응답한다.
6. 두 이웃 `shelfRank` 사이에 더 끼워넣을 문자열 여유가 없어지는 극단적인 경우, 서버가 해당 사용자 서재 전체의 `shelfRank`를 넓은 간격으로 재계산(rebalance)한다. 이 재계산은 별도로 노출되는 API가 아니라 서버 내부 유지보수 동작이다.
7. `GET /api/v1/library/books`의 `sortBy`에 `SHELF_ORDER`를 추가하고 기본값으로 삼는다(기존 기본값 `CREATED_AT`을 대체). `sortOrder` 기본값도 `DESC`에서 `ASC`로 바꿔, 파라미터 없이 호출해도 사용자가 재배열한 서재 순서가 자연스러운 진열 순서(오름차순)로 나오게 한다. `LibraryBookSummary`에 `shelfRank`를 필수 필드로 추가한다.

## 결과

- ADR-0002가 정의했던 `bookNumber`(정수, `updateLibraryBook`으로 직접 수정 가능)는 이 ADR로 대체된다.
- `.harness/DOMAIN.md`의 "미결정 도메인" 중 "`bookNumber`의 사용자별 유일성과 재정렬 규칙" 항목이 해소되어 결정된 사항으로 옮겨졌다.
- 재정렬 API가 도메인 언어("이 책 뒤로 옮겨줘")로 노출되어, 클라이언트가 정렬 키의 내부 표현(LexoRank 문자열)을 알거나 계산할 필요가 없다.
- 목록 조회 기본 정렬이 `SHELF_ORDER`/`ASC`로 바뀌어, 별도 파라미터 없이도 서재 화면이 사용자가 재배열한 순서를 그대로 보여준다. 기존에 `CREATED_AT`/`DESC` 기본값에 의존하던 클라이언트가 있다면 명시적으로 해당 파라미터를 지정해야 한다.
- `shelfRank` 생성·삽입·rebalance 로직 구현은 아직 시작하지 않았다 — `.harness/PLAN.md`(LibraryBook 도메인/영속성)에 반영했다.
