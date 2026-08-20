package com.chc.dpgb;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

// @DataJpaTest는 JPA 관련 autoconfiguration만 골라 로드하고 Flyway는 포함하지 않는다 —
// ddl-auto: validate가 실제 마이그레이션된 스키마를 보게 하려면 명시적으로 가져와야 한다.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(TestcontainersConfiguration.class)
public abstract class RepositoryIntegrationTestSupport {
}
