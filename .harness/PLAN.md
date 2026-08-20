# PLAN (미완료 계획)

완료된 항목은 여기 체크만 남기지 않고 `STATE.md`로 옮긴 뒤 이 문서에서 제거한다.

## 공통 계약 인프라

`com.chc.dpgb.common.ErrorResponse{code, message}`는 인증 기반(CLIAR-28) 작업에서 이미 만들어졌다 — 재사용한다.

- [ ] `@RestControllerAdvice` 전역 예외 처리 — `openapi.yaml`의 각 `responses.*`(400/401/403/404/409/502)를 `ErrorResponse{code, message}`로 매핑하는 표준 예외 계층 설계. `com.chc.dpgb.security.JwtAuthenticationEntryPoint`(401 전용)와의 통합 여부도 이때 재검토
- [ ] endpoint별 stable error code(`INVALID_BOOK_DATA`, `LIBRARY_BOOK_NOT_FOUND`, `SCRAP_NOT_FOUND`, `LIBRARIAN_NOT_FOUND` 등)를 예외 타입과 1:1로 연결

## LibraryBook 도메인/영속성

`.harness/DOMAIN.md`에 정의된 업무 규칙을 구현 기준으로 삼는다(CLIAR-43에서 `genre`/`moodTags` 제거됨).

- [ ] `LibraryBook` aggregate 설계 — `memberId`, `bookId`, `bookNumber`, 확인된 메타데이터(title/author/isbn/publisher/publishedDate/coverUrl), `totalPages`, `currentPage`, `ReadingStatus`, 생성/수정 시각
- [ ] 페이지·상태 불변식 구현: `totalPages > 0`, `0 <= currentPage <= totalPages`, `currentPage==0→NOT_STARTED`, `0<currentPage<totalPages→READING`, `currentPage==totalPages→COMPLETED`, 진도율 `currentPage/totalPages*100`, 전체 페이지를 기존 현재 페이지보다 작게 줄이는 것은 금지, 이전 페이지로의 이동(정정)은 허용
- [ ] 사용자별 중복 판정: ISBN이 있으면 사용자별 ISBN 우선, 없으면 정규화된 제목+저자 보조 기준, 동시 등록 대비 DB unique 제약 병행
- [ ] JPA Repository + `RepositoryIntegrationTestSupport`(`@DataJpaTest` + Testcontainers, 아직 미생성 — 첫 Repository 테스트 작성 시 신설) 기반 통합 테스트

## Library CRUD API

- [ ] `POST /api/v1/library/books`(`createLibraryBook`) — 등록, 중복 시 409
- [ ] `GET /api/v1/library/books`(`getLibraryBooks`) — author/readingStatus 필터, sortBy(`TITLE`/`AUTHOR`/`CREATED_AT`/`PROGRESS`)/sortOrder, page/size 페이징 (XToMany fetch join 금지 원칙 준수)
- [ ] `GET /api/v1/library/books/{bookId}`(`getLibraryBook`) — 소유자 검증(403)·404 처리
- [ ] `PATCH /api/v1/library/books/{bookId}`(`updateLibraryBook`) — 부분 수정, 누락 필드는 기존값 유지
- [ ] `DELETE /api/v1/library/books/{bookId}`(`deleteLibraryBook`) — 204
- [ ] 각 endpoint의 `operationId`·요청/응답 스키마·status를 기준으로 MockMvc 계약 테스트 작성

## Reading Progress API

- [ ] `PATCH /api/v1/library/books/{bookId}/progress`(`updateReadingProgress`) — currentPage/totalPages 갱신, 진도율·상태 서버 계산, `currentPage > totalPages` 시 400(`INVALID_PAGE_VALUE`)

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
