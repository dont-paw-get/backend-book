# ADR-0008: 책장(Shelf) 관리 신규 도입, `shelfRank` 범위를 책장별로 재조정

- 상태: Accepted
- 일자: 2026-08-21

## 배경

사용자가 다음 기능표를 제공하며 책장(서재 내 책을 그룹으로 묶는 단위) 관리 기능 추가를 요청했다: 책장 생성, 책장에 책 넣기(default 책장 존재, 다른 책장으로 이동 가능), 책장 수정(이름 변경), 책장 삭제(내부 책은 default 책장으로 이동), 책장별 책 목록 조회, 책장 목록 조회.

이 기능은 이미 구현되어 있는 `LibraryBook`/`shelfRank`(ADR-0004, CLIAR-31) 설계와 직접 부딪힌다. `shelfRank`는 지금까지 `memberId`(사용자) 전역에서 유일한 진열 순서 키였는데, 책장이 여러 개가 되면 "책장별 책 목록 조회"가 그 책장 안에서의 순서를 보여줘야 자연스럽다 — 사용자 전역 순서를 그대로 두면 한 책장 안에 다른 책장 책들의 순서 값이 섞여 있어 "이 책장 안에서 몇 번째"라는 개념이 애매해진다.

## 결정

1. `Shelf` aggregate를 신설한다: `shelfId`, `memberId`, `name`, `isDefault`(서버 전용 플래그), 생성·수정 시각.
2. `LibraryBook`에 `shelfId`(소속 책장, 필수)를 추가한다. `createLibraryBook`에서 `shelfId`를 생략하면 사용자의 기본 책장에 배치한다.
3. **`shelfRank`의 유일성 범위를 `memberId` 전역에서 `shelfId`별로 좁힌다.** DB unique 제약을 `(memberId, shelfRank)`에서 `(shelfId, shelfRank)`로 변경한다(후속 구현 티켓에서 반영). 서로 다른 책장의 책은 `shelfRank` 값이 같아도 무방하다.
4. `reorderLibraryBook`(`beforeBookId`/`afterBookId`)은 같은 책장에 속한 책끼리만 지정할 수 있다 — 다른 책장에 속한 책을 지정하면 기존과 동일하게 400(`INVALID_REORDER_TARGET`)이다.
5. 책장 간 이동은 재정렬과 분리된 전용 API `PATCH /api/v1/library/books/{bookId}/shelf`(`moveLibraryBookToShelf`)로 한다. 이동한 책은 대상 책장의 맨 뒤(`ShelfRank.after`)에 배치되고, 이전 책장에서의 순서는 유지되지 않는다. 대상 책장이 존재하지 않거나 이 사용자 소유가 아니면 400(`INVALID_SHELF_TARGET`) — `reorderLibraryBook`의 `INVALID_REORDER_TARGET`과 같은 패턴("참조로 넘긴 대상이 유효하지 않다"는 이 리소스에 대한 입력 오류)이다.
6. 기본 책장은 계정 생성 이벤트 없이 필요 시점에 서버가 자동 생성한다(get-or-create). 이 서비스는 회원가입/계정 생성 이벤트를 소유하지 않으므로, 그 사용자의 책장 관련 동작(서재 책 등록, 책장 목록 조회 등)이 처음 필요해지는 시점에 없으면 만든다.
7. 기본 책장은 삭제할 수 없다 — 시도하면 400(`DEFAULT_SHELF_CANNOT_BE_DELETED`). 이름 변경은 허용한다 — "기본"이라는 성질은 `isDefault` 플래그로만 구분되고 이름과는 무관하다.
8. 책장을 삭제하면 그 안에 있던 모든 책이 사용자의 기본 책장 맨 뒤로 이동한다.
9. 책장 이름 중복은 서버가 막지 않는다 — ISBN 없는 `LibraryBook`의 제목·저자 중복을 사용자 자율에 맡긴 ADR-0007과 같은 기조다.
10. 신규 endpoint 5개를 추가한다: `POST /api/v1/library/shelves`(`createShelf`), `GET /api/v1/library/shelves`(`getShelves`), `PATCH /api/v1/library/shelves/{shelfId}`(`updateShelf`), `DELETE /api/v1/library/shelves/{shelfId}`(`deleteShelf`), `GET /api/v1/library/shelves/{shelfId}/books`(`getShelfBooks`).
11. 기존 `GET /api/v1/library/books`(`getLibraryBooks`)는 하위 호환을 위해 전체 책장 합산 조회로 유지하되, 선택적 `shelfId` 쿼리 필터를 추가한다.
12. `CreateLibraryBookResponse`, `LibraryBookSummary`, `LibraryBookDetailResponse`에 `shelfId`를 노출한다.

## 결과

- `.harness/DOMAIN.md`에 `Shelf` aggregate 절 신설, `LibraryBook`/`shelfRank` 절에 `shelfId` 범위 변경 반영.
- `docs/api/openapi.yaml`(v0.6.0)에 `Shelf` 태그, 신규 endpoint 5개 + `moveLibraryBookToShelf`, 관련 스키마·오류 응답 추가.
- **이미 구현된 코드(CLIAR-31)에 대한 후속 변경이 필요하다:** `LibraryBook` entity에 `shelfId` 컬럼 추가, `LibraryBookRepository`/`LibraryBookJpaRepository`/`LibraryBookRepositoryJpaAdapter`의 조회 메서드를 책장 범위로 변경, `V2__create_library_book.sql`의 unique 제약을 `member_id+shelf_rank`에서 `shelf_id+shelf_rank`로 변경하는 신규 Flyway migration. 이 ADR은 계약 수준 결정이며, 실제 코드 변경은 `.harness/PLAN.md`의 "책장(Shelf) 관리 API" 섹션에 구현 체크리스트로 남긴다.
- 목록 조회가 이제 "전체 책장 합산"과 "특정 책장"이라는 두 층위를 갖게 됐다 — 프론트엔드가 서재 전체 뷰와 책장별 뷰를 구분해 호출해야 한다.
