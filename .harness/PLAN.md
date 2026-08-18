# PLAN (미완료 계획)

완료된 항목은 여기 체크만 남기지 않고 `STATE.md`로 옮긴 뒤 이 문서에서 제거한다.

## PostgreSQL 전환

- [ ] `build.gradle`에서 `com.h2database:h2`, `spring-boot-h2console` 제거
- [ ] `org.postgresql:postgresql` (운영/개발), Testcontainers PostgreSQL 모듈 (`org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`) 추가
- [ ] `application.yaml`에 datasource(PostgreSQL) 설정과 profile 분리 추가
- [ ] Flyway 도입 여부와 최초 migration 파일 결정
- [ ] `TestcontainersConfiguration` 빈 구성 (`withReuse(true)`, `src/test/resources/testcontainers.properties`)

## Gradle 통합 테스트 태스크

- [ ] `integrationTest` source set/task 구성 (`test`와 분리)
- [ ] `check`가 `test`와 `integrationTest`를 모두 실행하도록 연결
- [ ] `RepositoryIntegrationTestSupport`(`@DataJpaTest` + Testcontainers), `IntegrationTestSupport`(`@SpringBootTest` + Testcontainers) 기반 클래스는 첫 Repository/전체 컨텍스트 테스트 작성 시점에 함께 생성

## 기타

- [ ] `.kiro/steering`에서 관리하던 product/domain/architecture 정책을 다시 문서화할지, `.harness`로 완전히 이전할지 결정
