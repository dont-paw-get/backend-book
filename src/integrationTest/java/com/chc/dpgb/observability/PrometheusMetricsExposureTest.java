package com.chc.dpgb.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.chc.dpgb.IntegrationTestSupport;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * 작업 1(infra 연동)의 완료 기준을 코드로 고정한다: /actuator/prometheus 스크레이프 출력에
 * http_server_requests_seconds_count / _bucket 이 있고 application="backend-book" 공통 라벨이 붙는다.
 * (percentiles-histogram 이 꺼져 있으면 _bucket 이 없어 infra 의 p99 알림이 동작하지 않는다.)
 */
@AutoConfigureMockMvc
class PrometheusMetricsExposureTest extends IntegrationTestSupport {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PrometheusMeterRegistry prometheusMeterRegistry;

    @Test
    void HTTP_요청_메트릭을_히스토그램과_application_공통태그로_Prometheus_포맷으로_노출한다() throws Exception {
        // 관측 대상 요청을 한 건 발생시킨다 — 인증이 없어 401 이어도 http.server.requests 메트릭은 기록된다.
        mockMvc.perform(get("/api/v1/librarians"));

        String scrape = prometheusMeterRegistry.scrape();

        assertThat(scrape)
                .contains("http_server_requests_seconds_count")
                .contains("http_server_requests_seconds_bucket")
                .contains("application=\"backend-book\"");
    }
}
