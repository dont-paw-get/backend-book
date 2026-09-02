package com.chc.dpgb.common.observability;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

import io.micrometer.observation.ObservationPredicate;

@Configuration(proxyBeanMethods = false)
class ObservabilityConfiguration {

    /**
     * 관측(span·메트릭) 대상에서 제외할 요청 경로. 정확히 일치하거나 이 값 바로 아래 하위 경로면 제외한다.
     * k8s readiness/liveness 프로브(`/health`, pod당 10~20초 간격)와 Swagger 문서 자산(`/docs`·`/webjars`·`/openapi.yaml`),
     * Prometheus ServiceMonitor 스크레이핑(`/actuator`, 30초 간격)은 비즈니스 트래픽이 아닌데 자동 계측이 매 요청 span을
     * 만들어, dev sampling 1.0에서 Tempo를 인프라 trace로 도배한다.
     * `/health`·`/openapi.yaml`·`/docs`·`/webjars`는 SecurityConfig의 permitAll 목록과 같다. `/actuator`는 별도 관리 포트(8081,
     * dev overlay 전용)로 서비스되어 메인 SecurityFilterChain을 타지 않으므로 permitAll 목록에는 없다.
     */
    private static final List<String> UNOBSERVED_PATHS =
            List.of("/health", "/openapi.yaml", "/docs", "/webjars", "/actuator");

    /**
     * inbound HTTP 서버 관측만 필터한다. 다른 종류의 Observation(RestClient outbound, JDBC, 커스텀 span 등)은 그대로 통과시킨다.
     * predicate가 false를 반환하면 해당 Observation은 no-op이 되어 span과 `http.server.requests` 메트릭이 모두 생기지 않는다.
     */
    @Bean
    ObservationPredicate noiseFilteringObservationPredicate() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext serverContext) {
                return !isUnobserved(serverContext.getCarrier().getRequestURI());
            }
            return true;
        };
    }

    private static boolean isUnobserved(String path) {
        return UNOBSERVED_PATHS.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }
}
