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
