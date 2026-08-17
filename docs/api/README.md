# Book Service API 문서

이 디렉터리는 API 계약을 탐색하고 사용하는 진입점이다. request/response schema와 endpoint 목록을 README에 복제하지 않는다.

## 문서
- [`openapi.yaml`](./openapi.yaml): 유일한 API wire 계약
- [`decisions/`](./decisions/): 계약 변경의 배경과 결정
- [API 참조 steering](../../.kiro/steering/book-library-api.md): Kiro context 연결

계약 내용이 충돌하면 `openapi.yaml`을 우선한다. 제품 범위는 `product.md`, 서비스 경계는 `architecture.md`, 업무 규칙은 `domain.md`, 테스트 방식은 `tdd.md`가 각각 소유한다.

## 사용
- 백엔드 구현과 MockMvc 계약 테스트는 OpenAPI의 `operationId`, schema와 responses를 기준으로 한다.
- 프론트엔드·BFF·다른 서비스는 OpenAPI로 client, mock 또는 타입을 생성할 수 있다.

## 변경
API를 변경할 때는 OpenAPI를 먼저 계약 변경의 중심으로 삼고 `.kiro/steering/artifact-synchronization.md`의 영향 범위를 함께 갱신한다. 호환성을 깨거나 정책 근거가 필요한 변경은 `decisions/`에 ADR을 추가한다.

현재 정규화의 근거는 [`0001-contract-normalization.md`](./decisions/0001-contract-normalization.md)를 참고한다.
