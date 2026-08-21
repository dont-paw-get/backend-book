# PLAN (미완료 계획)

완료된 항목은 여기 체크만 남기지 않고 `STATE.md`로 옮긴 뒤 이 문서에서 제거한다.

## 책장(Shelf) 관리 API (신규, CLIAR 번호 미배정 — 계약은 ADR-0008로 확정, 구현 대기)

계약(`docs/api/openapi.yaml` v0.6.0, `.harness/DOMAIN.md`, ADR-0008)은 확정·반영 완료했다 — 상세는 `.harness/STATE.md` 참조. 아래는 구현 체크리스트만 남긴다.

- [ ] `Shelf` entity/영속성(포트/어댑터 패턴, `LibraryBookRepository`와 동일한 구조 — `ShelfRepository`/`ShelfJpaRepository`/`ShelfRepositoryJpaAdapter`) + 기본 책장 get-or-create 로직
- [ ] 기존 `LibraryBook`/`LibraryBookRepository`/`LibraryBookJpaRepository`/`LibraryBookRepositoryJpaAdapter`/`V2__create_library_book.sql`에 `shelf_id` 컬럼 추가, unique 제약을 `member_id+shelf_rank`에서 `shelf_id+shelf_rank`로 변경(신규 Flyway migration, `V2`는 아직 배포 전이므로 직접 수정 가능한지 배포 여부 먼저 확인)
- [ ] `POST /api/v1/library/shelves`(`createShelf`), `GET /api/v1/library/shelves`(`getShelves`, `bookCount` 집계 포함), `PATCH /api/v1/library/shelves/{shelfId}`(`updateShelf`), `DELETE /api/v1/library/shelves/{shelfId}`(`deleteShelf`, 내부 책을 기본 책장으로 이동), `GET /api/v1/library/shelves/{shelfId}/books`(`getShelfBooks`) controller/service
- [ ] `PATCH /api/v1/library/books/{bookId}/shelf`(`moveLibraryBookToShelf`) — 대상 책장 끝에 `ShelfRank.after`로 배치
- [ ] `createLibraryBook`에 선택적 `shelfId` 반영(생략 시 기본 책장), `getLibraryBooks`에 선택적 `shelfId` 필터 반영
- [ ] `reorderLibraryBook`이 같은 책장 내 이웃만 허용하도록 검증 로직 확인(현재 구현은 사용자 전역 기준이라 책장 범위로 좁혀야 함)
- [ ] 도메인 `IllegalArgumentException`(불변식 위반)을 `common.exception` 타입(`InvalidShelfDataException`/`ShelfAccessDeniedException`/`ShelfNotFoundException`/`DefaultShelfCannotBeDeletedException`/`InvalidShelfTargetException` 등, 신규 concrete 예외 필요)으로 번역
- [ ] 각 endpoint의 계약 테스트 작성(기본 책장 자동 생성, 삭제 시 이동, 책장 범위 재정렬 실패 케이스 포함)

## Library CRUD API

`LibraryBook`/`ShelfRank`/`LibraryBookRepository`(포트)는 CLIAR-31에서 구현 완료(`.harness/STATE.md` 참조). 서비스 계층은 `LibraryBookRepository` 포트에만 의존하고, JPA 파생 쿼리 메서드명(`LibraryBookJpaRepository`, package-private)은 몰라도 된다. 이 섹션은 그 위에 controller/service(유스케이스)를 배선한다.

**주의:** ADR-0008(위 "책장(Shelf) 관리 API" 섹션)로 `shelfRank`의 유일성 범위가 `memberId` 전역에서 `shelfId`별로 바뀌었다. 이 섹션을 구현하기 전에 `LibraryBook`에 `shelf_id` 컬럼과 그 범위로 재정의된 unique 제약이 먼저 반영되어 있어야 아래 항목들이 성립한다(순서: Shelf 컬럼/제약 변경 → 이 섹션).

- [ ] `POST /api/v1/library/books`(`createLibraryBook`) — 등록, 중복 시 409. `shelfId` 생략 시 기본 책장에 배치. `shelfRank`는 그 책장의 `ShelfRank.after(마지막 shelfRank)`로 서버가 부여(책장이 비어 있으면 `ShelfRank.initial()`)
- [ ] `GET /api/v1/library/books`(`getLibraryBooks`) — 선택적 `shelfId` 필터(생략 시 전체 책장 합산), author 필터, sortBy(`SHELF_ORDER`/`TITLE`/`AUTHOR`/`CREATED_AT`/`PROGRESS`, 기본값 `SHELF_ORDER`)/sortOrder(기본값 `ASC`), page/size 페이징 (XToMany fetch join 금지 원칙 준수)
- [ ] `GET /api/v1/library/books/{bookId}`(`getLibraryBook`) — 소유자 검증(403)·404 처리
- [ ] `PATCH /api/v1/library/books/{bookId}`(`updateLibraryBook`) — 7개 필드 항상 전체 포함(ADR-0006, `Scrap.updateScrap`과 동일 방식), `isbn`/`publisher`/`publishedDate`/`coverUrl`는 `null`=삭제. `shelfRank`/`shelfId`는 이 endpoint로 변경할 수 없다(순서 변경은 아래 reorder, 책장 이동은 위 `moveLibraryBookToShelf` 참조)
- [ ] `PATCH /api/v1/library/books/{bookId}/order`(`reorderLibraryBook`) — `beforeBookId`/`afterBookId` 중 하나로 이웃 지정, `ShelfRank.between(...)`으로 새 값 계산·저장. 대상이 같은 책장에 없거나 자기 자신이면 400(`INVALID_REORDER_TARGET`). `ShelfRank.between`이 `ShelfRankExhaustedException`을 던지면 이 유스케이스가 `LibraryBookRepository.findShelfOrderedByRank`(책장 범위) + `ShelfRank.rebalancedSequence`로 그 책장 전체를 재계산해 저장한 뒤 재시도 — 이 오케스트레이션 지점을 이 티켓에서 결정·구현한다
- [ ] `DELETE /api/v1/library/books/{bookId}`(`deleteLibraryBook`) — 204
- [ ] 도메인 계층의 `IllegalArgumentException`(불변식 위반)을 어떤 `common.exception` 타입(`InvalidBookDataException`/`InvalidPageValueException` 등)으로 번역할지는 이 서비스/컨트롤러 계층에서 결정 — LibraryBook 엔티티 자체는 HTTP 관심사와 분리되어 있음
- [ ] 각 endpoint의 `operationId`·요청/응답 스키마·status를 기준으로 MockMvc 계약 테스트 작성

## Reading Progress API

- [ ] `PATCH /api/v1/library/books/{bookId}/progress`(`updateReadingProgress`) — currentPage/totalPages 갱신, 진도율 서버 계산, `currentPage > totalPages` 시 400(`INVALID_PAGE_VALUE`)

## Book Discovery API (외부 연동)

알라딘 API 연동이 필요하다(CLIAR-43에서 OCR/AI 분석은 범위에서 삭제됨 — 다른 컴포넌트 담당). 자격 증명·endpoint가 아직 없으므로 어댑터 인터페이스 + 스텁 구현으로 먼저 격리하고, 실제 연동은 자격 증명 확보 시점에 스텁을 교체한다.

- [ ] `GET /api/v1/books/search`(`searchBookInfo`) — 알라딘 API 어댑터(title/author 검색), 결과 없으면 200 + 빈 배열, 응답 필드는 알라딘이 제공하는 항목(title/author/isbn13/publisher/pub_date/cover_url)으로 한정
- [ ] `author` 정규화 — 알라딘 응답의 "이름 (역할)" 결합 문자열에서 역할 라벨("지은이"/"옮긴이"/"편역" 등)을 제거하고 이름만 쉼표로 join해 반환하는 로직(어댑터 또는 별도 정규화 컴포넌트)
- [ ] `totalPages`는 알라딘 응답에 없는 경우가 대부분이므로 어댑터가 값을 억지로 채우지 않고 생략(optional) 처리
- [ ] 어댑터는 인터페이스로 분리해 계약 테스트에서 목킹 가능하게 함

## Scrap CRUD API (신규, CLIAR-43)

`.harness/DOMAIN.md`의 Scrap aggregate 규칙을 구현 기준으로 삼는다. 스크랩은 LibraryBook 소유자(`memberId`) 기준으로 접근 권한을 검증한다.

- [ ] `Scrap` aggregate 설계 — `scrapId`, `bookId`, `sentence`, `pageNumber`(nullable), `memo`(nullable), 생성/수정 시각
- [ ] `POST /api/v1/library/books/{bookId}/scraps`(`createScrap`) — 생성, LibraryBook 소유자 아니면 403/404
- [ ] `GET /api/v1/library/books/{bookId}/scraps`(`getScraps`) — 책별 목록, page/size 페이징
- [ ] `GET /api/v1/library/scraps/{scrapId}`(`getScrap`) — 상세 조회
- [ ] `PATCH /api/v1/library/scraps/{scrapId}`(`updateScrap`) — `sentence`/`pageNumber`/`memo` 항상 전체 포함, `pageNumber`/`memo`에 `null` = 삭제, 값 = 교체. `sentence`는 `null` 불가
- [ ] `DELETE /api/v1/library/scraps/{scrapId}`(`deleteScrap`) — 204
- [ ] 각 endpoint의 계약 테스트 작성(소유권 403/404 케이스 포함)

## Librarian API (신규, CLIAR-43)

`.harness/DOMAIN.md`의 Librarian aggregate 규칙을 구현 기준으로 삼는다.

- [ ] `Librarian`(마스터 데이터) aggregate — `librarianId`, `name`, `type`, `imageUrl`, `evolutionStage`. 초기 데이터(고양이/새/곰/달팽이 등) 시드 방법 결정(Flyway migration 데이터 삽입 등)
- [ ] `MemberLibrarianSelection` aggregate — `memberId` → `librarianId`, 선택 시각. 회원당 최대 1개, 재선택 시 덮어쓰기
- [ ] `GET /api/v1/librarians`(`getLibrarians`) — 마스터 목록 조회
- [ ] `GET /api/v1/members/me/librarian`(`getMyLibrarian`) — 미선택 시 404(`LIBRARIAN_NOT_SELECTED`)
- [ ] `PUT /api/v1/members/me/librarian`(`selectMyLibrarian`) — 선택/변경, 존재하지 않는 `librarianId`는 404(`LIBRARIAN_NOT_FOUND`)
- [ ] 각 endpoint의 계약 테스트 작성

## 계약 테스트 전수화

- [ ] `openapi.yaml`의 모든 `operationId` × `responses` 조합에 대응하는 MockMvc 테스트 커버리지 확보 (ADR-0001 원칙)
