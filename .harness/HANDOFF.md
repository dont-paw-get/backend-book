# HANDOFF (세션별 서술 로그, append-only)

## 2026-08-18

`AGENTS.md`가 다른 프로젝트(Aiverse, MySQL, Jira 무관 feature 브랜치, `backend` 하위 모듈)의 흔적을 그대로 담고 있어서, 사용자와 대화하며 이 저장소(Book Service, Java/Spring, PostgreSQL 예정)에 맞게 하나씩 정책을 확정했다.

확정한 내용:
- DB는 PostgreSQL 단일 기준, H2 완전 제거 (Python RAG 서비스는 별도 PostgreSQL+pgvector 소유)
- `test`/`integrationTest` 분리, `check`가 둘 다 실행
- `.harness` 6개 문서 체계 도입, 계획 절차 예외 정의
- 브랜치 `{티켓번호}-{설명}`, `develop` 기준 PR, 사용자 병합, 커밋은 루트 `README.md`(CLIAR-20) 컨벤션 그대로
- JPA/테스트 기반 클래스 원칙은 문서화하되 아직 없는 클래스는 "필요 시 생성"으로 표기

`AGENTS.md` 전체를 재작성하고 `.harness/{ARCHITECTURE,STATE,PLAN,DECISIONS,HANDOFF,BACKLOG}.md`를 새로 생성했다. 상세 이유는 `DECISIONS.md`, 남은 작업은 `PLAN.md` 참조.

아직 하지 않은 것: `build.gradle`/`application.yaml`의 실제 PostgreSQL·Testcontainers 반영, `integrationTest` Gradle 태스크 구성, `.kiro` 삭제로 사라진 product/domain/architecture 스티어링 문서를 다시 만들지 여부 결정. 커밋도 아직 하지 않았다 — 현재 브랜치(`CLIAR-9-Steering-Scaffolding`) 위에서 파일만 생성된 상태이며 사용자 확인 후 커밋할 것.

다음 세션 시작 시: 이 파일 다음으로 `STATE.md`, `ARCHITECTURE.md`, `PLAN.md` 순서로 읽고, PostgreSQL 전환 작업을 계획할지 사용자에게 먼저 확인한다.
