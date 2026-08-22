# PLAN (미완료 계획)

완료된 항목은 여기 체크만 남기지 않고 `STATE.md`로 옮긴 뒤 이 문서에서 제거한다.

## 계약 테스트 전수화

- [ ] `openapi.yaml`의 모든 `operationId` × `responses` 조합에 대응하는 MockMvc 테스트 커버리지 확보 (ADR-0001 원칙). Library/Shelf(CLIAR-32)는 컨트롤러 자체 검증(필수 필드 누락 등)과 대표 성공 경로만 `@WebMvcTest`로 다뤘고, 403/404/409 등 서비스가 던지는 예외 경로는 `LibraryBookServiceTest`/`ShelfServiceTest`(Mockito 단위 테스트) 수준에서만 검증했다 — MockMvc 계약 테스트로도 전수화할지는 이 섹션에서 판단
