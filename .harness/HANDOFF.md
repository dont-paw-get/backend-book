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

## 2026-08-18 (계속): PostgreSQL 전환 구현 (CLIAR-26)

`CLIAR-26-DB-PostgreSQL-전환` 브랜치(이미 체크아웃되어 있었음)에서 `.harness/PLAN.md`의 "PostgreSQL 전환" 체크리스트를 실행했다. H2 제거, PostgreSQL/Flyway/Testcontainers 의존성 추가, `application.yaml`+`application-local.yaml`+`application-prod.yaml` 프로필 분리, `db/migration/V1__init.sql` baseline, `docker-compose.yml`을 추가했고, H2 제거로 기존 `DpgbApplicationTests`가 DB 없이 못 뜨는 문제 때문에 "Gradle 통합 테스트 태스크" 섹션(원래 별도 계획)도 같이 처리해 `integrationTest` source set/task, `TestcontainersConfiguration`, `IntegrationTestSupport`를 만들고 그 테스트를 옮겼다. 이유와 상세는 `DECISIONS.md`(2026-08-18, "PostgreSQL 전환 — Flyway 채택...") 참조.

작업 중 발견한 문제와 우회: `io.spring.dependency-management`가 Spring Boot 4.1.0 BOM의 testcontainers-bom 중첩 import를 반영하지 못했고, 그 BOM이 가리키는 testcontainers 2.0.5도 아직 Maven Central에 없어서, 실재하는 1.21.3을 `build.gradle`에 직접 고정했다(주석으로 이유 남김). Boot/Testcontainers 업그레이드 시 재검토 필요 — `.harness/BACKLOG.md`에는 아직 안 옮겼음, 다음 세션에서 필요하면 옮길 것.

로컬에 Docker Desktop이 설치는 되어 있었지만 데몬이 꺼져 있어 직접 기동시킨 뒤 `./gradlew test`(NO-SOURCE 통과), `./gradlew integrationTest`(실제 Postgres 컨테이너로 Flyway+Spring 컨텍스트 기동 성공), `./gradlew check`까지 모두 통과를 확인했다. 커밋은 아직 하지 않았다 — 사용자 확인 후 커밋할 것.

다음 세션 시작 시: `.harness/PLAN.md`의 다음 섹션("인증 기반")을 계획할지 사용자에게 먼저 확인한다.
