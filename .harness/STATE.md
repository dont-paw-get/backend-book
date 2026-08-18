# STATE (완료 스냅샷)

단계가 끝나면 그 단계를 한 줄로 갱신한다. 세션별 서술은 `HANDOFF.md`에 남긴다.

## 완료된 단계

- 프로젝트 골격: Spring Boot 4.1.0 / Java 21 애플리케이션 초기 생성, `DpgbApplicationTests` smoke test 존재.
- API 계약 정규화: `docs/api/openapi.yaml` 및 ADR-0001(`docs/api/decisions/0001-contract-normalization.md`) 수립.
- 개발 하네스 전환: 기존 `.kiro/steering` 산출물을 삭제하고 `AGENTS.md` + `.harness/*` 체계로 통합. DB는 PostgreSQL 단일 기준으로 확정, H2 제거 결정.
- API 계약 보완(CLIAR-10): `docs/api/openapi.yaml`에서 대응 필드 없는 `coverColors` 필터 제거, `language`/`coverUrl`/`bookNumber` 스키마 불일치 수정(ADR-0002). `docs/api/README.md`의 삭제된 `.kiro/steering` 문서 참조를 `AGENTS.md`/`.harness/ARCHITECTURE.md`로 정리. 오늘의 기분 추천·문장 OCR/감상/비밀 메모는 이 저장소(Book Service) 범위 밖(다른 MSA 컴포넌트 담당)으로 확인.
- 구현 계획 수립: 인증 기반 → 공통 계약 인프라 → LibraryBook 도메인/영속성 → Library CRUD → Reading Progress → Book Discovery(어댑터+스텁) → 계약 테스트 전수화 순서를 `.harness/PLAN.md`에 확정. 업무 규칙 문서 `.harness/DOMAIN.md` 신설(삭제된 `.kiro/steering/domain.md` 내용을 이 저장소 범위로 재정리), `CLAUDE.md` 단일 소유권 표에 반영.
- PostgreSQL 전환 + Gradle `integrationTest` 태스크 구성(CLIAR-26): `build.gradle`에서 H2 제거, `org.postgresql:postgresql`/Flyway(`flyway-core`, `flyway-database-postgresql`)/Testcontainers(`org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql` 1.21.3 고정 — Spring Boot 4.1.0 BOM이 가리키는 2.0.5가 아직 Maven Central 미게시) 추가. `application.yaml`(공통) + `application-local.yaml`(Docker Postgres 기본값) + `application-prod.yaml`(env var, 기본값 없음) 프로필 분리, `spring.jpa.hibernate.ddl-auto: validate` + Flyway 활성화, 최초 baseline `db/migration/V1__init.sql`(빈 마이그레이션) 추가. 로컬 Postgres용 `docker-compose.yml` 신설. `integrationTest` source set/task를 새로 구성하고 `check`에 연결, `TestcontainersConfiguration`(`@ServiceConnection` + `withReuse(true)`)과 `IntegrationTestSupport`(`@SpringBootTest`) 기반 클래스를 `src/integrationTest`에 신설, 기존 `DpgbApplicationTests`를 `src/test`에서 `src/integrationTest`로 옮겨 `IntegrationTestSupport`를 상속하도록 전환(더 이상 DB 없이 컨텍스트가 뜰 수 없으므로). `./gradlew test`, `./gradlew integrationTest`(Docker Desktop 기동 후 실제 컨테이너로 검증 완료), `./gradlew check` 모두 통과 확인. `RepositoryIntegrationTestSupport`(`@DataJpaTest`)는 첫 Repository 테스트 작성 시점까지 보류.

## 미완료 / 진행 중

`.harness/PLAN.md` 참조.
