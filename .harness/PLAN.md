# PLAN (미완료 계획)

완료된 항목은 여기 체크만 남기지 않고 `STATE.md`로 옮긴 뒤 이 문서에서 제거한다.

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

- [ ] `openapi.yaml`의 모든 `operationId` × `responses` 조합에 대응하는 MockMvc 테스트 커버리지 확보 (ADR-0001 원칙). Library/Shelf(CLIAR-32)는 컨트롤러 자체 검증(필수 필드 누락 등)과 대표 성공 경로만 `@WebMvcTest`로 다뤘고, 403/404/409 등 서비스가 던지는 예외 경로는 `LibraryBookServiceTest`/`ShelfServiceTest`(Mockito 단위 테스트) 수준에서만 검증했다 — MockMvc 계약 테스트로도 전수화할지는 이 섹션에서 판단
