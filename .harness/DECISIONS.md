# DECISIONS (결정 이력, 최신이 위)

## 2026-08-18: PostgreSQL 전환 — Flyway 채택, `integrationTest` 태스크·기반 클래스 동시 구성

- **Flyway 채택:** `spring.jpa.hibernate.ddl-auto`를 `validate`로 고정하고 스키마 변경은 Flyway migration(`src/main/resources/db/migration`)으로만 한다. 이유: 운영 DB 스키마를 Hibernate 자동 생성에 맡기지 않고 명시적 이력으로 남기는 편이 이 팀의 ADR 중심 작업 방식(`docs/api/decisions/`)과 일관된다. 아직 도메인 엔티티가 없어 최초 `V1__init.sql`은 빈 baseline이며, 첫 테이블은 `LibraryBook` aggregate 구현과 함께 다음 migration에서 추가한다.
- **`integrationTest` 태스크를 PostgreSQL 전환과 같은 작업에서 구성:** `.harness/PLAN.md`는 원래 "PostgreSQL 전환"과 "Gradle 통합 테스트 태스크"를 별도 섹션으로 분리해뒀지만, H2를 제거하고 나면 기존 `DpgbApplicationTests`(`@SpringBootTest`)가 실제 DB 없이는 컨텍스트를 못 띄운다. `ARCHITECTURE.md`가 이미 "datasource/Flyway 도입 시 이 스위트를 옮긴다"고 명시해뒀으므로, `integrationTest` source set/task, `TestcontainersConfiguration`, `IntegrationTestSupport`를 함께 만들고 `DpgbApplicationTests`를 `src/integrationTest`로 옮겼다. `RepositoryIntegrationTestSupport`(`@DataJpaTest`)는 아직 Repository가 없으므로 만들지 않고 보류했다 — "필요해지면 만든다" 원칙 유지.
- **Testcontainers 버전 직접 고정:** `io.spring.dependency-management`(1.1.7)가 `spring-boot-dependencies`(4.1.0)의 `testcontainers-bom` 중첩 import를 반영하지 못해 `org.testcontainers:junit-jupiter`/`postgresql` 버전을 못 찾는 문제가 있었고, Boot 4.1.0이 가리키는 `testcontainers.version=2.0.5`는 Maven Central에 아직 게시되지 않은 상태였다(직접 조회로 확인, 현재 Central 최신은 1.21.3). 두 문제를 동시에 우회하기 위해 `build.gradle`에 실재하는 1.21.3을 직접 명시했다. Boot 업그레이드나 Testcontainers 2.x 정식 게시 시 재검토 필요.
- **로컬 Postgres는 `docker-compose.yml`로 제공:** PLAN 체크리스트에는 없었지만 `AGENTS.md`/DB 정책이 "로컬은 Docker로 PostgreSQL 실행"을 못박고 있고, `application-local.yaml` 기본값(계정 `dpgb`/`dpgb`, 5432 포트)이 이 파일과 짝을 이뤄야 의미가 있어 함께 추가했다.
- **검증:** Docker Desktop을 로컬에서 기동해 `./gradlew test`(NO-SOURCE 통과), `./gradlew integrationTest`(실제 Postgres 컨테이너 기동 + Flyway + Spring 컨텍스트 기동 성공), `./gradlew check`(둘 다 실행) 모두 통과를 실제로 확인했다.

## 2026-08-18: AGENTS.md를 Book Service에 맞게 재작성하고 `.harness` 체계 도입

- **DB:** Book Service는 전용 PostgreSQL만 사용한다. H2(`com.h2database:h2`, `spring-boot-h2console`)는 제거한다. 이유: RAG 벡터 검색을 위해 별도 Python 서비스가 PostgreSQL+pgvector를 쓰기로 했고, 팀 전체가 하나의 DB 엔진을 공유하면 운영 지식과 로컬 환경을 통일할 수 있다. H2와 PostgreSQL의 SQL 방언·제약조건·동시성 차이가 크고, 사용자별 중복 방지·동시 등록 같은 이 서비스의 핵심 불변식은 PostgreSQL로만 신뢰성 있게 검증 가능하다.
- **서비스 경계:** Book Service(이 저장소, Java)와 Python RAG Service는 각자 PostgreSQL을 소유하고 DB를 직접 공유하지 않는다. 데이터 교환은 API 또는 event로 한다.
- **테스트 태스크 분리:** 빠른 `test`(DB 없음)와 PostgreSQL Testcontainers 기반 `integrationTest`를 분리한다. `check`와 CI는 항상 둘 다 실행한다. 이유: 로컬 반복 개발 속도와 PostgreSQL 실제 검증을 모두 확보하기 위함.
- **`.harness` 도입:** Claude Code/Codex/Kiro를 번갈아 사용할 예정이므로 `HANDOFF/STATE/ARCHITECTURE/PLAN/DECISIONS/BACKLOG` 6개 문서로 크로스 툴 연속성을 관리한다. `.kiro/steering`은 삭제된 상태이며 API 계약 산출물(`docs/api/*`)만 계속 별도로 소유한다.
- **계획 절차의 예외:** 파일을 바꾸지 않는 설명/조사/리뷰와 명백한 소규모 수정은 `PLAN.md` 초안 없이 즉시 수행한다. 기능·DB·API·아키텍처 변경만 계획 승인을 거친다. 이유: 매 요청에 계획 절차를 강제하면 단순 질문까지 느려진다.
- **브랜치/커밋:** 브랜치명은 `{티켓번호}-{설명}` 형식으로 통일하고 기존 `feature/{번호}-{한글}` 규칙은 폐기한다. `develop`에서 분기해 PR을 `develop`으로 생성하고 사용자가 병합한다. 커밋 컨벤션은 저장소 `README.md`(`CLIAR-20`)의 실제 팀 규칙(영어 타입 + 한국어 제목, `[티켓번호]`, scope)을 그대로 따른다. 문서 전용 작업도 티켓 브랜치에서 진행하며 main 직접 커밋 예외는 두지 않는다. 자동 병합·삭제·push는 하지 않는다. 이유: 기존 `AGENTS.md`가 이 프로젝트가 아닌 다른 프로젝트(Aiverse, MySQL, Jira 무관 feature 브랜치)의 흔적이었고, 실제 Git 이력(`CLIAR-*` 커밋, `main`/`develop` 브랜치, README 컨벤션)과 맞지 않았다.
- **JPA/테스트 기반 클래스:** N+1 방지, fetch join, DTO 분리 원칙은 지금부터 문서화하지만, 아직 존재하지 않는 `RepositoryIntegrationTestSupport`/`IntegrationTestSupport` 같은 기반 클래스는 "필요해지면 만든다"로 서술하고 이미 존재하는 것처럼 쓰지 않는다. 이유: 아직 entity/Repository가 없는 초기 골격 단계에서 존재하지 않는 클래스를 실재하는 것처럼 서술하면 다른 도구가 잘못된 전제로 코드를 찾게 된다.
