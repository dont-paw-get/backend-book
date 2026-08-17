---
name: virtual-shelf-feature
description: Book Service의 OpenAPI operation 하나를 작은 vertical slice로 구현하거나 변경할 때 사용한다.
---
# Book Service 기능 구현

## 입력 수집
- wire 계약: `docs/api/openapi.yaml`
- 서비스 경계: `.kiro/steering/architecture.md`
- 업무 규칙: `.kiro/steering/domain.md`
- 기술 관례: `.kiro/steering/java-spring.md`

필요한 operation과 관련 규칙만 읽고 문서 내용을 skill 안에 복제하지 않는다.

## 실행
1. operation의 성공 또는 실패 동작 하나를 선택한다.
2. `tdd.md`의 한 사이클을 수행한다.
3. domain → application → adapter/api 중 필요한 최소 vertical slice만 변경한다.
4. 외부 공급자는 port 대역으로 시작하고 실제 adapter는 별도 동작으로 구현한다.
5. `artifact-synchronization.md`에 따라 파생 산출물을 갱신한다.
6. `spring-boot-verification` skill로 검증한다.

## 결과
선택한 동작, 변경 계층, 갱신한 산출물과 미검증 외부 연동을 한국어로 보고한다.
