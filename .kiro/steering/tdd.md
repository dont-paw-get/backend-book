---
inclusion: always
---
# TDD 원칙

이 문서는 **Red → Green → Refactor 방식과 테스트 계층·품질 기준**만 소유한다. API acceptance는 OpenAPI, 업무 사례는 `domain.md`, 실행 명령은 `spring-boot-verification` skill을 따른다.

## 한 사이클
1. 외부에서 관찰 가능한 동작 하나를 선택한다.
2. 가장 작은 테스트를 작성하고 의도한 이유로 실패하는 Red를 확인한다.
3. 테스트를 통과시키는 최소 production code로 Green을 만든다.
4. 동작을 유지하며 이름, 중복과 경계를 Refactor한다.
5. 관련 테스트가 계속 통과하는지 확인한 뒤 다음 동작으로 이동한다.

컴파일 실패만으로 Red를 대신하지 않는다. 처음부터 통과한 테스트는 새로운 동작을 검증하는지 다시 확인한다.

## 테스트 계층
- **Domain unit**: Spring 없이 값과 상태 전이를 검증한다.
- **Application unit**: 유스케이스 결과를 검증하고 outbound port는 deterministic fake 또는 최소 mock으로 대체한다.
- **Web slice**: HTTP 계약, 인증, validation과 오류 매핑을 검증한다.
- **Persistence slice**: mapping, 쿼리, 제약과 동시성 경계를 검증한다.
- **Outbound adapter**: timeout, 공급자 오류와 잘못된 payload 변환을 검증한다.
- **Smoke**: 애플리케이션 context의 최소 기동을 보호한다.

규칙을 검증할 수 있는 가장 낮은 계층을 선택하고 모든 테스트에서 Spring context를 띄우지 않는다.

## 테스트 작성 규칙
- 이름에 기대 행위와 조건을 드러낸다.
- Given/When/Then 또는 Arrange/Act/Assert를 읽을 수 있게 유지한다.
- 시간·UUID·외부 응답을 제어 가능하게 주입한다.
- private method와 구현 세부사항 대신 공개 행위를 검증한다.
- entity와 값 객체를 과도하게 mock하지 않는다.
- 테스트 간 DB·정적 상태·실행 순서를 공유하지 않는다.
- flaky test를 재실행으로 숨기지 않는다.

## 완료 기준
- 새 동작의 Red를 실제로 관찰했다.
- 최소 구현과 Refactor 후 관련 테스트가 Green이다.
- 변경 범위에 맞는 회귀 검증을 수행했다.
- 자동 검증하지 못한 외부 연동과 위험을 보고했다.
