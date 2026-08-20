# ARCHITECTURE (현재 상태)

이 문서는 지금 시점의 실제 기술 스택·구조·컨벤션만 담는다. 결정 이유는 `DECISIONS.md`, 진행 상황은 `STATE.md`를 본다.

## 기술 스택

- Java 21, Spring Boot 4.1.0, Gradle Wrapper 9.5.1
- Spring MVC, Spring Data JPA, Flyway(`flyway-core`, `flyway-database-postgresql` + Boot autoconfigure를 가져오는 `spring-boot-starter-flyway` — 이 starter 없이는 `FlywayAutoConfiguration`이 로드되지 않아 마이그레이션이 자동 실행되지 않는다, CLIAR-31에서 발견)
- Spring Security OAuth2 Resource Server(`spring-boot-starter-oauth2-resource-server`) — JWT 검증, 인증 서비스는 AWS Cognito User Pool
- 기준 패키지: `com.chc.dpgb`
- DB: PostgreSQL (JDBC 드라이버 `org.postgresql:postgresql`, 스키마는 Flyway migration으로 관리, `spring.jpa.hibernate.ddl-auto: validate`)
- Testcontainers(`org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql`) — 버전은 `build.gradle` 주석 참조. `io.spring.dependency-management`가 Spring Boot BOM의 testcontainers-bom 중첩 import를 반영하지 못하고, Boot 4.1.0이 가리키는 testcontainers.version이 아직 Maven Central에 없어 실재하는 버전을 직접 고정했다. Boot 업그레이드 시 재검토.
- Lombok (compile/annotation processor) — entity getter는 `@Getter`로 생성(예: `LibraryBook`), setter는 쓰지 않고 불변식이 있는 도메인 메서드로만 상태를 바꾼다
- 실제 버전은 `build.gradle`과 Gradle Wrapper가 최종 기준
- **Spring Boot 4.1.0 패키지 이동 주의**: Jackson은 `com.fasterxml.jackson.databind`가 아니라 `tools.jackson.databind`(Jackson 3)를 쓴다. `@WebMvcTest`는 `org.springframework.boot.webmvc.test.autoconfigure`(`spring-boot-webmvc-test` 모듈)에 있다. 예전 Boot 버전 예제 코드의 import 경로를 그대로 쓰면 컴파일 에러가 난다.

## 저장소 구조

루트 단일 Gradle 프로젝트다. `backend` 하위 모듈은 없다. `test`(단위)와 `integrationTest`(PostgreSQL Testcontainers) source set이 분리되어 있다.

```text
src/main/java/com/chc/dpgb
├─ DpgbApplication.java
├─ common
│  ├─ ErrorResponse.java              # {code, message} — API 전역 에러 응답 포맷
│  └─ exception
│     ├─ DomainException.java         # abstract, code() 추상 메서드 — 계층 최상위
│     ├─ BadRequestException.java     # abstract, 400
│     ├─ ForbiddenException.java      # abstract, 403
│     ├─ NotFoundException.java       # abstract, 404
│     ├─ ConflictException.java       # abstract, 409
│     ├─ BadGatewayException.java     # abstract, 502
│     ├─ GlobalExceptionHandler.java  # @RestControllerAdvice — 5개 abstract 타입 + 500 fallback(INTERNAL_ERROR)을 ErrorResponse로 매핑
│     └─ (stable error code별 concrete 예외 13종 — InvalidSearchParameterException 등, openapi.yaml의 components.responses.* 기준)
├─ library
│  ├─ LibraryBook.java                   # aggregate root(JPA entity) — register/updateMetadata/updateProgress/changeShelfRank에 불변식 캡슐화
│  ├─ LibraryBookRepository.java         # 포트(순수 인터페이스, Spring Data 비의존) — 서비스 계층이 의존하는 도메인 메서드명
│  ├─ LibraryBookJpaRepository.java      # Spring Data JPA 인터페이스(package-private) — 파생 쿼리 메서드명, 포트 구현체 내부에서만 사용
│  ├─ LibraryBookRepositoryJpaAdapter.java  # @Repository, LibraryBookRepository 구현 — LibraryBookJpaRepository로 위임
│  ├─ ShelfRank.java                     # LexoRank pure 유틸(Spring 비의존) — initial/before/after/between/rebalancedSequence
│  └─ ShelfRankExhaustedException.java   # 키 공간 소진 내부 신호(API 미노출) — rebalance 트리거용
└─ security
   ├─ SecurityConfig.java             # JwtDecoder/SecurityFilterChain/AuthenticationEntryPoint 빈
   ├─ JwtAuthenticationEntryPoint.java
   ├─ MemberIdResolver.java           # Jwt의 sub 클레임 → memberId 추출 단일 지점
   └─ jwt
      ├─ TokenUseValidator.java       # token_use == access
      └─ ClientIdValidator.java       # client_id == 등록된 App Client

src/main/resources
├─ application.yaml          # 공통 설정 (JPA, Flyway 활성화, OAuth2 Resource Server issuer-uri/app-client-id)
├─ application-local.yaml    # 로컬 프로필 — docker-compose Postgres 기본값
├─ application-prod.yaml     # 운영 프로필 — 전부 env var, 기본값 없음
└─ db/migration
   ├─ V1__init.sql                     # baseline (빈 마이그레이션)
   └─ V2__create_library_book.sql      # library_book 테이블 + unique 제약(member_id+shelf_rank, member_id+isbn partial — isbn 없는 도서는 중복판정 안 함, ADR-0007)

src/test/java/com/chc/dpgb
├─ common/exception/GlobalExceptionHandlerTest.java
├─ library/ShelfRankTest.java
├─ library/LibraryBookTest.java
└─ security/...  # validator/MemberIdResolver 단위 테스트, SecurityConfigTest

src/integrationTest/java/com/chc/dpgb
├─ TestcontainersConfiguration.java       # @TestConfiguration, PostgreSQLContainer + @ServiceConnection, withReuse(true)
├─ IntegrationTestSupport.java            # @SpringBootTest + TestcontainersConfiguration import
├─ RepositoryIntegrationTestSupport.java  # @DataJpaTest + AutoConfigureTestDatabase(NONE) + @ImportAutoConfiguration(FlywayAutoConfiguration) + TestcontainersConfiguration import
├─ DpgbApplicationTests.java              # IntegrationTestSupport 상속 (smoke test)
└─ library/LibraryBookRepositoryTest.java # RepositoryIntegrationTestSupport 상속 — 저장/조회, unique 제약, 소유권 스코프

src/integrationTest/resources
└─ testcontainers.properties  # testcontainers.reuse.enable=true

docker-compose.yml  # 로컬 개발용 PostgreSQL (POSTGRES_DB/USER/PASSWORD=dpgb)
```

## 서비스 경계

이 저장소는 Book Service(Java, 이 프로젝트)이며, 독립된 Python RAG Service와 별도로 개발된다.

Book Service는 다른 Java 기반 MSA 서비스들과 PostgreSQL 인스턴스·데이터베이스 하나를 공유한다(CLIAR-43, `.harness/DECISIONS.md` 참조). 각 서비스는 자신의 schema는 자신의 Flyway migration으로만 관리하지만, 다른 서비스의 schema를 직접 JOIN해서 조회할 수 있다 — 이 저장소 안에서는 아직 다른 서비스의 schema/테이블 이름이 정해지지 않았고, 실제 JOIN 쿼리도 없다. Python RAG Service는 이 공유 DB에 포함되지 않고 자체 PostgreSQL + pgvector를 계속 별도로 소유하며, 데이터 공유는 API 또는 event로만 한다.

## 테스트 구조

- `test`: 단위 테스트. DB 없음. `com.chc.dpgb.security` 패키지의 validator/`MemberIdResolver` 단위 테스트와 `SecurityConfigTest`/`GlobalExceptionHandlerTest`(`@WebMvcTest` + 테스트 전용 nested 컨트롤러), `com.chc.dpgb.library`의 `ShelfRankTest`/`LibraryBookTest`(Domain unit, Spring 컨텍스트 없음)가 있다.
- `integrationTest`: PostgreSQL Testcontainers 기반 통합 테스트. Gradle에 구성 완료 — `./gradlew integrationTest`로 단독 실행, `./gradlew check`가 `test`와 함께 실행. Docker(Docker Desktop 등)가 로컬에 떠 있어야 한다.
- `RepositoryIntegrationTestSupport`(`@DataJpaTest`)가 CLIAR-31에서 처음 만들어졌다. `@DataJpaTest`의 큐레이션된 autoconfiguration 목록은 Flyway를 포함하지 않으므로 `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`를 명시적으로 추가해야 `ddl-auto: validate`가 마이그레이션된 실제 스키마를 검증한다(`.harness/DECISIONS.md` 참조). 새 `*RepositoryImpl`을 추가할 때 이 기반 클래스를 상속한다.
- 통합 테스트: `DpgbApplicationTests`(`IntegrationTestSupport` 상속, 빈 smoke test), `LibraryBookRepositoryTest`(`RepositoryIntegrationTestSupport` 상속).

## API 문서

- wire 계약: `docs/api/openapi.yaml`
- 사용 안내: `docs/api/README.md`
- 계약 결정: `docs/api/decisions/`

## Git

- 원격: `origin` = `https://github.com/dont-paw-get/backend-book.git`
- 브랜치: `main`(릴리스), `develop`(통합), `{티켓번호}-{설명}`(작업)
- 커밋 컨벤션: 저장소 루트 `README.md` 참조
