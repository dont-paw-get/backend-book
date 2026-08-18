## 하네스: 크로스 툴 작업 연속성

**목표:** Claude Code, Codex, Kiro 중 어떤 도구로 세션을 시작하든, 이전 작업 맥락(계획·진행상황·인수인계)을 이어서 파악하고 작업할 수 있게 한다.

**세션 시작 시 반드시 먼저 읽을 것 (이 순서로):**

1. `.harness/HANDOFF.md` — 직전 세션이 어디서 멈췄는지
2. `.harness/STATE.md` — 지금까지 무엇이 완료되었는지
3. `.harness/ARCHITECTURE.md` — 기술 스택/구조 요약 (코드베이스 재탐색 최소화)
4. `.harness/PLAN.md` — 제안·확정·진행 중인 계획
5. 필요 시 `.harness/DECISIONS.md`(과거 결정 이유), `.harness/BACKLOG.md`(미해결 항목)

**문서별 책임 (중복 기록 금지 — 아래 표에 없는 문서에는 해당 내용을 쓰지 않는다):**

| 문서 | 담는 내용 | 담지 않는 내용 |
| --- | --- | --- |
| `HANDOFF.md` | 세션마다 무엇을 했는지 (append-only 서술형 로그) | 단계별 완료 요약(STATE 몫), 결정 이유(DECISIONS 몫) |
| `STATE.md` | 지금까지 끝난 것의 단계 단위 요약 스냅샷 | 세션별 서술(HANDOFF 몫). 이슈 하나하나를 로그처럼 쌓지 않는다 — 단계가 끝나면 그 단계 한 줄로 갱신 |
| `ARCHITECTURE.md` | 지금의 기술 스택/폴더 구조/컨벤션 (현재 상태) | 왜 그렇게 정했는지(DECISIONS 몫), 진행 상황(STATE 몫) |
| `DECISIONS.md` | 결정 내용과 이유의 역사(append-only) | 구현 여부/진행 상황(STATE 몫) |
| `PLAN.md` | 아직 안 끝난 계획과 체크리스트만 | 완료된 항목(체크만 남기지 말고 STATE로 옮긴 뒤 제거) |
| `BACKLOG.md` | 지금 하지 않지만 나중에 할 것(버그·기술부채·아이디어) | 진행 중인 계획(PLAN 몫) |

API wire 계약과 계약 결정은 `.harness`가 아니라 `docs/api/openapi.yaml`, `docs/api/README.md`, `docs/api/decisions/`가 소유한다. `.harness`는 이 산출물을 복제하지 않고 참조한다.

**작업 워크플로우 (필수):**

- 새로운 기능/변경 요청을 받으면, 바로 구현하지 말고 `.harness/PLAN.md`에 계획 초안을 작성한다.
- 사용자에게 계획을 제시하고 피드백을 받아 반영하는 과정을 반복한다.
- 사용자가 명시적으로 컨펌하면 계획을 확정 상태로 바꾸고 구현을 시작한다.
- 다음은 계획 절차 없이 바로 수행한다: 설명·조사·코드 리뷰처럼 파일을 변경하지 않는 요청, 오탈자나 명백한 단순 수정(수정 전 무엇을 바꾸는지 한 줄로 알린다).
- `PLAN.md` 단계별 체크리스트 항목은 하나씩 구현이 끝날 때마다 즉시 `.harness/STATE.md`에 반영하고, 그 항목을 `PLAN.md`에서 제거한다. `STATE.md`에는 위 표대로 단계 단위 한 줄 요약만 남기고 세션 서술은 남기지 않는다.
- 구현 완료 후 `.harness/STATE.md`를 갱신한다.
- 세션을 종료하거나 작업을 중단할 때 `.harness/HANDOFF.md`에 다음 세션을 위한 인수인계를 남긴다.
- 아키텍처/워크플로우에 대한 중요한 결정을 내리면 `.harness/DECISIONS.md` 표의 최상단에 이유와 함께 기록해 최신 결정이 위에 오도록 유지한다.

**트리거:** 이 프로젝트에서의 모든 작업 요청에 위 워크플로우를 적용하라. 단순 질문(코드 설명 등)은 하네스 절차 없이 바로 응답 가능.

## 하네스: DB 정책

**목표:** 서비스별 DB 소유권과 로컬·테스트·운영에서 사용할 DB 엔진을 고정한다.

- Book Service(이 저장소)의 유일한 RDB는 PostgreSQL이다. 로컬 실행, 테스트, 운영 모두 PostgreSQL을 사용한다.
- H2는 사용하지 않는다. `com.h2database:h2`, `spring-boot-h2console` 의존성은 제거 대상이다.
- 별도로 개발되는 Python RAG 서비스는 자체 PostgreSQL + pgvector를 소유한다. 두 서비스는 DB 인스턴스를 공유하더라도 database·계정·migration·데이터 소유권을 분리하고, 서로의 테이블을 직접 조회하지 않는다. 데이터 공유가 필요하면 공개 API 또는 합의된 event를 사용한다.
- 로컬 개발 환경의 PostgreSQL은 Docker로 실행한다. 운영 접속 정보는 비밀값으로 주입하고 코드나 설정 파일에 기본값을 넣지 않는다.

## 하네스: 테스트 실행 정책

**목표:** Claude Code, Codex, Kiro 중 어떤 도구로 작업하든 검증 시 동일한 테스트 범위를 적용한다.

- 이 저장소는 루트 단일 Gradle 프로젝트다. `backend` 하위 디렉터리는 없다. 모든 명령은 저장소 루트에서 실행한다 (Windows는 `gradlew.bat`, 범용 표기는 `./gradlew`).
- 구현·수정 후 **기본 검증**은 **단위 테스트만** 실행한다 (`./gradlew test`).
- **TDD로 통합 테스트를 작성·수정하는 작업** 중에는 해당 통합 테스트를 반드시 실행한다 — red → green → refactor 사이클을 지킨다. 해당 테스트 클래스만 실행해도 된다 (예: `./gradlew integrationTest --tests com.chc.dpgb.library.LibraryBookRepositoryTest`).
- **통합 테스트 전체 스위트**(`./gradlew integrationTest`)와 **전체 검증**(`./gradlew check`)은 사용자가 명시적으로 요청했거나 CI에서 실행한다. CI의 `check`는 `test`와 `integrationTest`를 모두 실행해 PostgreSQL 검증이 누락되지 않게 한다.
- 단위 테스트: Domain unit, Application unit(Mockito 또는 fake), `@WebMvcTest` 등 DB나 통합 테스트 베이스를 상속하지 않는 테스트.
- Repository/persistence 통합 테스트: PostgreSQL Testcontainers 기반 `@DataJpaTest` 슬라이스.
- 전체 컨텍스트 통합 테스트: PostgreSQL Testcontainers 기반 `@SpringBootTest` — 현재는 `DpgbApplicationTests` 하나뿐이며, datasource/Flyway 도입 시 이 스위트로 옮긴다.
- 현재 `integrationTest` source set/task는 아직 Gradle에 구성되어 있지 않다. 실제로 필요해지는 시점(첫 Repository 또는 Flyway 도입)에 구성하고, 구성 후에만 이 문서에서 "존재하는 태스크"로 서술한다. 그 전까지는 `.harness/PLAN.md`의 계획 항목으로 관리한다.

## 하네스: 통합 테스트 구조

**목표:** PostgreSQL 실제 동작 검증은 유지하면서 통합 테스트 기동 비용을 줄인다.

| 베이스 클래스 | 어노테이션 | 용도 |
| --- | --- | --- |
| `RepositoryIntegrationTestSupport` | `@DataJpaTest` + Testcontainers(PostgreSQL) | `*RepositoryTest` — JPA·Flyway만 기동 (Security·Web·OpenAPI 제외) |
| `IntegrationTestSupport` | `@SpringBootTest` + Testcontainers(PostgreSQL) | 앱 기동 검증, Security 필터 체인, MockMvc 전체 스택 |

이 두 기반 클래스는 아직 코드로 존재하지 않는다. 첫 Repository 테스트 또는 첫 전체 컨텍스트 통합 테스트를 작성할 때 위 구조대로 실제로 만든다. 이미 만들어졌다고 가정하고 참조하지 않는다.

- Testcontainers PostgreSQL은 Spring `@Bean`으로 관리하는 `TestcontainersConfiguration`에서 구성하고, `withReuse(true)` + `src/test/resources/testcontainers.properties`(`testcontainers.reuse.enable=true`)로 컨테이너를 재사용한다.
- Repository 통합 테스트 작성 시 `RepositoryIntegrationTestSupport`를 상속한다. 새 `*RepositoryImpl` 추가 시 관련 테스트 설정에 등록한다.
- 테스트별 데이터 격리는 `@Transactional` 롤백을 유지한다.

## 하네스: 테스트 작성 원칙 (결과 검증 우선)

**목표:** 테스트가 내부 구현이 아니라 관찰 가능한 최종 결과(반환값·상태·예외)를 검증하게 하여, 테스트 리팩토링 내구성을 강화한다.

- 단위 테스트는 반환값, 변경된 상태, 발생한 예외를 검증하는 것을 기본으로 한다. Mock의 `verify()`는 "부작용이 실행됐는지/안 됐는지" 확인에만 쓴다.
- Mock으로는 결과 자체를 관찰할 수 없는 경우(동시성 등)는 실제 인프라(PostgreSQL Testcontainers)를 쓰는 통합 테스트로 결과를 검증한다.
- 제어 불가능한 값(현재 시각, 난수 등)은 로직 내부에서 직접 얻지 않고 파라미터로 주입받아 결정론적으로 동작하게 한다.
- 도메인 객체는 계산 후 상태 변경/값 반환까지만 책임지고, 저장·외부 호출 같은 부작용은 그 바깥 계층이 담당한다. 이렇게 분리하면 도메인 로직은 Mock 없이 입력→출력만으로 검증 가능하다.
- private 메서드/함수는 직접 테스트하지 않는다. 검증이 필요할 만큼 복잡해졌다면 별도 단위(클래스/함수)로 뽑아 공개 계약으로 노출한다.
- 쿼리·SQL에 비결정적 함수나 하드코딩된 조건(기간 등)을 넣지 않고 파라미터로 받는다. 테스트는 쿼리의 "파라미터 → 결과" 계약을 검증하고, 쿼리 구현 자체(문법, 최적화 방식)는 검증 대상으로 삼지 않는다.

## 하네스: JPA 조회 최적화

- 연관관계 조회에서 N+1 문제가 발생하면 명시적 JPQL(또는 도입 시 Querydsl) fetch join으로 해결한다. `@EntityGraph`는 사용하지 않는다.
- 페이징 조회에서는 `XToMany` 컬렉션 fetch join을 사용하지 않는다. DB 페이징이 아닌 메모리 페이징과 중복 행 문제가 발생할 수 있다.
- 페이징 목록 쿼리는 `ManyToOne`·`OneToOne` 등 `XToOne` 연관관계만 fetch join한다.
- API 응답 DTO는 목록용과 상세용으로 분리한다. 목록 DTO는 페이징 쿼리가 `XToOne` 연관관계만으로 완성되도록 설계하고, `XToMany` 컬렉션이 필요한 응답은 상세 조회 DTO에서 제공한다.
- 목록에서 컬렉션 정보가 반드시 필요하면 컬렉션 fetch join 대신 별도 일괄 조회, DTO projection, 집계 쿼리 등 페이징을 보존하는 방식을 사용한다.
- Querydsl은 아직 이 저장소에 도입되지 않았다. 도입한다면 이 절과 `build.gradle`, 관련 테스트 설정을 같은 작업에서 함께 갱신한다.

## 하네스: 브랜치·커밋·병합 전략

**목표:** Claude Code, Codex, Kiro 중 어떤 도구로 작업을 시작하든 팀의 실제 Jira 흐름과 커밋 컨벤션을 동일하게 따른다.

- 원격 저장소는 `origin` (`https://github.com/dont-paw-get/backend-book.git`)이며 `main`과 `develop`을 갖는다.
- 작업 브랜치는 `develop`에서 분기하고 이름은 `{티켓번호}-{설명}` 형식을 사용한다 (예: `CLIAR-9-Steering-Scaffolding`). `feature/...` 형식은 사용하지 않는다.
- 현재 브랜치가 진행 중인 작업과 다른 티켓이면, 구현 시작 전에 해당 티켓 번호로 새 브랜치를 `develop`에서 생성하고 전환한다. 이미 해당 티켓 브랜치에 있다면 새 브랜치를 만들지 않는다.
- 문서·설정 전용 작업도 코드 변경과 동일하게 티켓 브랜치에서 진행한다. main에 직접 커밋하는 예외는 두지 않는다.
- 커밋 메시지 컨벤션은 저장소 루트 `README.md`(`CLIAR-20`)를 그대로 따른다.
  - `<타입>[적용 범위(선택)]: <제목>` 구조. 타입은 영어(`feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`), 제목/본문은 한국어.
  - 제목은 명사형 어미로 끝내고 50자 이내, 마침표 없음.
  - scope는 작업한 도메인(예: `curation`, `db`)을 명시하면 이력 추적에 유리하다.
  - 제목 또는 본문에 관련 티켓 번호를 `[CLIAR-9]`처럼 표기한다 (기존 이력 패턴).
- 커밋은 사용자가 명시적으로 요청했을 때만 생성한다. 관련 파일을 골라 stage하고, `git add .`/`git add -A`는 피한다.
- 한 티켓 브랜치에서 작업이 끝나면 PR을 `develop`으로 생성할 수 있지만, PR 생성과 push는 사용자가 명시적으로 요청했을 때만 수행한다.
- 브랜치 병합과 삭제는 자동으로 수행하지 않는다. PR 병합은 사용자가 직접 하거나, 사용자가 명시적으로 요청했을 때만 수행한다. `develop → main` 릴리스 병합도 동일하다.
- 강제 push, `reset --hard`, `clean -fd`, `branch -D` 등 destructive 작업은 사용자의 명시적 허락 없이 수행하지 않는다.
