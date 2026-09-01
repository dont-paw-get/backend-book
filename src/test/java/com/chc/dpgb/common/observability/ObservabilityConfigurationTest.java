package com.chc.dpgb.common.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ObservabilityConfigurationTest {

    @Test
    void local과_test_기본_sampling은_전량이다() throws IOException {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yaml"));

        assertThat(applicationYaml)
                .contains("sampling:")
                .contains("probability: 1.0");
    }

    @Test
    void dev_overlay는_sampling을_전량으로_설정한다() throws IOException {
        String configMapPatch = Files.readString(Path.of("k8s/overlays/dev/configmap-patch.yaml"));

        assertThat(configMapPatch)
                .contains("MANAGEMENT_TRACING_SAMPLING_PROBABILITY: \"1.0\"");
    }

    @Test
    void prod_overlay는_sampling을_10퍼센트로_설정한다() throws IOException {
        String configMapPatch = Files.readString(Path.of("k8s/overlays/prod/configmap-patch.yaml"));

        assertThat(configMapPatch)
                .contains("MANAGEMENT_TRACING_SAMPLING_PROBABILITY: '0.1'");
    }

    @Test
    void OTLP_endpoint와_protocol은_환경변수로만_주입한다() throws IOException {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yaml"));
        String applicationProdYaml = Files.readString(Path.of("src/main/resources/application-prod.yaml"));
        String baseConfigMap = Files.readString(Path.of("k8s/base/configmap.yaml"));

        assertThat(applicationYaml).doesNotContain("opentelemetry-collector");
        assertThat(applicationProdYaml).doesNotContain("opentelemetry-collector");
        assertThat(baseConfigMap)
                .contains("OTEL_SERVICE_NAME")
                .contains("OTEL_EXPORTER_OTLP_PROTOCOL: \"http/protobuf\"");
    }
}
