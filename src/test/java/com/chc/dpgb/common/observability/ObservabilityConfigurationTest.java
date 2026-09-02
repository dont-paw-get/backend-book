package com.chc.dpgb.common.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;

class ObservabilityConfigurationTest {

    private final ObservationPredicate predicate =
            new ObservabilityConfiguration().noiseFilteringObservationPredicate();

    @ParameterizedTest
    @ValueSource(strings = {
            "/health", "/openapi.yaml", "/docs", "/docs/index.html", "/webjars/swagger-ui/index.css",
            "/actuator", "/actuator/prometheus", "/actuator/health"})
    void 프로브와_문서와_메트릭_스크레이핑_경로_요청은_관측하지_않는다(String path) {
        assertThat(predicate.test("http.server.requests", serverContext(path))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/library/books", "/healthz", "/docs-internal", "/actuatorx", "/"})
    void 그_외_경로_요청은_관측한다(String path) {
        assertThat(predicate.test("http.server.requests", serverContext(path))).isTrue();
    }

    @Test
    void 서버_요청이_아닌_관측은_그대로_통과시킨다() {
        assertThat(predicate.test("book.discovery.search", new Observation.Context())).isTrue();
    }

    private static ServerRequestObservationContext serverContext(String path) {
        return new ServerRequestObservationContext(
                new MockHttpServletRequest("GET", path), new MockHttpServletResponse());
    }

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
                .contains("MANAGEMENT_TRACING_SAMPLING_PROBABILITY: '1.0'");
    }

    @Test
    void prod_overlay는_sampling을_10퍼센트로_설정한다() throws IOException {
        String configMapPatch = Files.readString(Path.of("k8s/overlays/prod/configmap-patch.yaml"));

        assertThat(configMapPatch)
                .contains("MANAGEMENT_TRACING_SAMPLING_PROBABILITY: '0.1'");
    }

    @Test
    void dev_overlay만_Prometheus_메트릭을_별도_관리_포트로_노출한다() throws IOException {
        String devConfigMapPatch = Files.readString(Path.of("k8s/overlays/dev/configmap-patch.yaml"));
        String prodConfigMapPatch = Files.readString(Path.of("k8s/overlays/prod/configmap-patch.yaml"));

        assertThat(devConfigMapPatch)
                .contains("MANAGEMENT_SERVER_PORT: '8081'")
                .contains("MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: 'health,prometheus'");
        assertThat(prodConfigMapPatch)
                .doesNotContain("MANAGEMENT_SERVER_PORT")
                .doesNotContain("MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE");
    }

    @Test
    void 공통_application_yaml은_actuator_web_노출을_비워_둔다() throws IOException {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yaml"));

        assertThat(applicationYaml)
                .contains("include: \"\"")
                .contains("application: backend-book")
                .contains("http.server.requests: true");
    }

    @Test
    void dev_overlay는_metrics_포트와_ServiceMonitor를_함께_추가한다() throws IOException {
        String servicePatch = Files.readString(Path.of("k8s/overlays/dev/service-patch.yaml"));
        String serviceMonitor = Files.readString(Path.of("k8s/overlays/dev/servicemonitor.yaml"));

        assertThat(servicePatch)
                .contains("name: metrics")
                .contains("targetPort: management");
        assertThat(serviceMonitor)
                .contains("kind: ServiceMonitor")
                .contains("path: /actuator/prometheus")
                .contains("port: metrics")
                .contains("interval: 30s");
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
