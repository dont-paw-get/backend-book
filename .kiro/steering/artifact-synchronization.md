---
inclusion: always
---
# 변경 산출물 동기화

이 문서는 **정보의 단일 소유권과 변경 영향 산출물 갱신 정책**만 소유한다.

## 단일 소유권
| 정보 | 소유 산출물 |
|---|---|
| 제품 목표·전체 사용자 흐름 | `.kiro/steering/product.md` |
| Book Service 책임·구조·통신 | `.kiro/steering/architecture.md` |
| aggregate·불변식·상태 전이 | `.kiro/steering/domain.md` |
| Java·Spring·JPA 관례 | `.kiro/steering/java-spring.md` |
| API wire 계약 | `docs/api/openapi.yaml` |
| API 문서 탐색·사용법 | `docs/api/README.md` |
| API 계약 결정과 근거 | `docs/api/decisions/` |
| 아키텍처 결정과 근거 | `docs/architecture/decisions/`(필요 시 생성) |
| TDD 원칙 | `.kiro/steering/tdd.md` |
| 구현 단계·의존 순서 | `.kiro/steering/delivery-workflow.md` |
| 보안·개인정보·AI 신뢰 경계 | `.kiro/steering/ai-privacy.md` |
| 응답 언어 | `.kiro/steering/korean-language.md` |
| 수동 검증 선택·결과 보고 | `spring-boot-verification` skill |
| 자동 검증·가드 실행 시점과 명령 | `.kiro/hooks/` |

다른 문서는 소유 내용을 반복하지 않고 소유 산출물을 참조한다. 실제 코드와 문서가 다르면 현재 상태와 목표 상태를 구분한다.

## 변경 영향
- 제품 흐름 변경: `product.md`, 관련 도메인·API, acceptance, 제품 다이어그램
- 서비스 경계·구조 변경: 실제 package/module, `architecture.md`, build·설정, architecture test, CI·배포, agent/skill 경로
- 도메인 변경: `domain.md`, domain/persistence test, entity·migration·DB 제약, 노출 API·event schema
- API 변경: OpenAPI, Controller·DTO·validation·오류 매핑, 계약 테스트, fixture·client·소비자 공지, 필요 시 ADR
- 외부 연동·설정 변경: port·adapter·configuration, 경계 테스트, `architecture.md`, 보안·운영·배포 문서
- 개발 정책 변경: 해당 steering, agent/skill/hook과 실행 문서

## 절차
1. 변경 전에 소유 산출물과 파생 산출물을 식별한다.
2. 동작 변경은 `tdd.md`에 따라 구현한다.
3. 코드와 관련 산출물을 같은 작업에서 갱신한다.
4. 오래된 경로, 이름, 예제, 버전과 상태 코드를 확인한다.
5. `spring-boot-verification` skill로 영향 범위를 검증한다.
6. 수정한 산출물, 검증 결과와 미검증 항목을 한국어로 보고한다.

저장소에 없는 첨부 흐름도 같은 산출물은 수정했다고 간주하지 않는다. 존재하지 않거나 수정할 수 없는 산출물은 이유와 권장 저장 경로를 보고한다.
