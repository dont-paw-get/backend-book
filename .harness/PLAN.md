# PLAN (미완료 계획)

완료된 항목은 여기 체크만 남기지 않고 `STATE.md`로 옮긴 뒤 이 문서에서 제거한다.

## Library CRUD API

`LibraryBook`/`ShelfRank`/`LibraryBookRepository`(포트)는 CLIAR-31에서 구현 완료(`.harness/STATE.md` 참조). 서비스 계층은 `LibraryBookRepository` 포트에만 의존하고, JPA 파생 쿼리 메서드명(`LibraryBookJpaRepository`, package-private)은 몰라도 된다. 이 섹션은 그 위에 controller/service(유스케이스)를 배선한다.

- [ ] `POST /api/v1/library/books`(`createLibraryBook`) — 등록, 중복 시 409. `shelfRank`는 `ShelfRank.after(마지막 shelfRank)`로 서버가 부여(서재가 비어 있으면 `ShelfRank.initial()`)
- [ ] `GET /api/v1/library/books`(`getLibraryBooks`) — author 필터, sortBy(`SHELF_ORDER`/`TITLE`/`AUTHOR`/`CREATED_AT`/`PROGRESS`, 기본값 `SHELF_ORDER`)/sortOrder(기본값 `ASC`), page/size 페이징 (XToMany fetch join 금지 원칙 준수)
- [ ] `GET /api/v1/library/books/{bookId}`(`getLibraryBook`) — 소유자 검증(403)·404 처리
- [ ] `PATCH /api/v1/library/books/{bookId}`(`updateLibraryBook`) — 7개 필드 항상 전체 포함(ADR-0006, `Scrap.updateScrap`과 동일 방식), `isbn`/`publisher`/`publishedDate`/`coverUrl`는 `null`=삭제. `shelfRank`는 이 endpoint로 변경할 수 없다(아래 reorder 참조)
- [ ] `PATCH /api/v1/library/books/{bookId}/order`(`reorderLibraryBook`) — `beforeBookId`/`afterBookId` 중 하나로 이웃 지정, `ShelfRank.between(...)`으로 새 값 계산·저장. 대상이 같은 서재에 없거나 자기 자신이면 400(`INVALID_REORDER_TARGET`). `ShelfRank.between`이 `ShelfRankExhaustedException`을 던지면 이 유스케이스가 `LibraryBookRepository.findShelfOrderedByRank` + `ShelfRank.rebalancedSequence`로 서재 전체를 재계산해 저장한 뒤 재시도 — 이 오케스트레이션 지점을 이 티켓에서 결정·구현한다
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
