# Book Service API 문서

이 디렉터리는 API 계약을 탐색하고 사용하는 진입점이다. request/response schema와 endpoint 목록을 README에 복제하지 않는다.

## 문서
- [`openapi.yaml`](./openapi.yaml): 유일한 API wire 계약
- [`decisions/`](./decisions/): 계약 변경의 배경과 결정

계약 내용이 충돌하면 `openapi.yaml`을 우선한다. 개발 하네스 워크플로우·DB·테스트·브랜치 정책은 루트 `AGENTS.md`, 서비스 경계·기술 스택 현황은 `.harness/ARCHITECTURE.md`가 소유한다.

## 사용
- 백엔드 구현과 MockMvc 계약 테스트는 OpenAPI의 `operationId`, schema와 responses를 기준으로 한다.
- 프론트엔드·BFF·다른 서비스는 OpenAPI로 client, mock 또는 타입을 생성할 수 있다.

## 변경
- [`0001-contract-normalization.md`](./decisions/0001-contract-normalization.md): 최초 계약 정규화 근거
- [`0002-library-book-schema-fixes.md`](./decisions/0002-library-book-schema-fixes.md): 서재 도서 스키마 불일치 수정 근거
- [`0003-scope-narrowing-and-new-resources.md`](./decisions/0003-scope-narrowing-and-new-resources.md): 장르/무드/`language` 제거, 알라딘 단일 소스화, OCR/AI 분석 삭제, 스크랩·동물 사서 추가, 이미지 파일 업로드 기능 제외 근거
- [`0004-shelf-rank-lexorank-ordering.md`](./decisions/0004-shelf-rank-lexorank-ordering.md): `bookNumber`(정수)를 LexoRank 기반 `shelfRank`(문자열)로 재설계, 전용 재정렬 endpoint 신설 근거
- [`0005-remove-reading-status.md`](./decisions/0005-remove-reading-status.md): `ReadingStatus`(`NOT_STARTED`/`READING`/`COMPLETED`) 필드·필터 전면 제거 근거
- [`0006-update-library-book-full-payload.md`](./decisions/0006-update-library-book-full-payload.md): `updateLibraryBook`을 부분 수정에서 `Scrap.updateScrap`과 동일한 전체 필드 포함(`null`=삭제) 방식으로 통일한 근거
- [`0007-drop-title-author-duplicate-check.md`](./decisions/0007-drop-title-author-duplicate-check.md): ISBN 없는 도서의 제목·저자 기반 중복 판정 제거 근거(ISBN 유일성 판정만 남김)
- [`0008-shelf-management.md`](./decisions/0008-shelf-management.md): 책장(Shelf) 관리 신규 도입, `shelfRank` 범위를 사용자 전역에서 책장별로 재조정한 근거
- [`0009-remove-representative-librarian-selection.md`](./decisions/0009-remove-representative-librarian-selection.md): 대표 사서 선택(`getMyLibrarian`/`selectMyLibrarian`)을 Member 서비스로 이관, Book Service는 사서 마스터 카탈로그(`getLibrarians`)만 유지
