# ARCHITECTURE (현재 상태)

이 문서는 지금 시점의 실제 기술 스택·구조·컨벤션만 담는다. 결정 이유는 `DECISIONS.md`, 진행 상황은 `STATE.md`를 본다.

## 기술 스택

- Java 21, Spring Boot 4.1.0, Gradle Wrapper 9.5.1
- Spring MVC, Spring Data JPA, Flyway(`flyway-core`, `flyway-database-postgresql`)
- 기준 패키지: `com.chc.dpgb`
- DB: PostgreSQL (JDBC 드라이버 `org.postgresql:postgresql`, 스키마는 Flyway migration으로 관리, `spring.jpa.hibernate.ddl-auto: validate`)
- Testcontainers(`org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql`) — 버전은 `build.gradle` 주석 참조. `io.spring.dependency-management`가 Spring Boot BOM의 testcontainers-bom 중첩 import를 반영하지 못하고, Boot 4.1.0이 가리키는 testcontainers.version이 아직 Maven Central에 없어 실재하는 버전을 직접 고정했다. Boot 업그레이드 시 재검토.
- Lombok (compile/annotation processor)
- 실제 버전은 `build.gradle`과 Gradle Wrapper가 최종 기준

## 저장소 구조

루트 단일 Gradle 프로젝트다. `backend` 하위 모듈은 없다. `test`(단위)와 `integrationTest`(PostgreSQL Testcontainers) source set이 분리되어 있다.

```text
src/main/java/com/chc/dpgb
└─ DpgbApplication.java

src/main/resources
├─ application.yaml          # 공통 설정 (JPA, Flyway 활성화)
├─ application-local.yaml    # 로컬 프로필 — docker-compose Postgres 기본값
├─ application-prod.yaml     # 운영 프로필 — 전부 env var, 기본값 없음
└─ db/migration
   └─ V1__init.sql           # baseline (아직 스키마 없음)

src/integrationTest/java/com/chc/dpgb
├─ TestcontainersConfiguration.java  # @TestConfiguration, PostgreSQLContainer + @ServiceConnection, withReuse(true)
├─ IntegrationTestSupport.java       # @SpringBootTest + TestcontainersConfiguration import
└─ DpgbApplicationTests.java         # IntegrationTestSupport 상속 (smoke test)

src/integrationTest/resources
└─ testcontainers.properties  # testcontainers.reuse.enable=true

docker-compose.yml  # 로컬 개발용 PostgreSQL (POSTGRES_DB/USER/PASSWORD=dpgb)
```

`src/test`에는 아직 단위 테스트가 없다(`./gradlew test`는 NO-SOURCE로 통과). `RepositoryIntegrationTestSupport`(`@DataJpaTest` + Testcontainers)는 첫 Repository 테스트 작성 시점에 `src/integrationTest`에 신설한다.

## 서비스 경계

이 저장소는 Book Service(Java, 이 프로젝트)이며, 독립된 Python RAG Service와 별도로 개발된다. 두 서비스는 각자 PostgreSQL을 소유하고 DB를 직접 공유하지 않는다.

## 테스트 구조

- `test`: 단위 테스트(Domain/Application unit, `@WebMvcTest`). DB 없음. 현재 테스트 없음(NO-SOURCE).
- `integrationTest`: PostgreSQL Testcontainers 기반 통합 테스트. Gradle에 구성 완료 — `./gradlew integrationTest`로 단독 실행, `./gradlew check`가 `test`와 함께 실행. Docker(Docker Desktop 등)가 로컬에 떠 있어야 한다.
- 현재 유일한 통합 테스트는 `DpgbApplicationTests`(`IntegrationTestSupport` 상속, 빈 smoke test).

## API 문서

- wire 계약: `docs/api/openapi.yaml`
- 사용 안내: `docs/api/README.md`
- 계약 결정: `docs/api/decisions/`

## Git

- 원격: `origin` = `https://github.com/dont-paw-get/backend-book.git`
- 브랜치: `main`(릴리스), `develop`(통합), `{티켓번호}-{설명}`(작업)
- 커밋 컨벤션: 저장소 루트 `README.md` 참조
