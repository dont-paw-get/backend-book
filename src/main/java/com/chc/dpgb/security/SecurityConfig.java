package com.chc.dpgb.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import com.chc.dpgb.security.jwt.CognitoAccessTokenValidator;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Prometheus 스크레이핑용 관리 포트(dev overlay 전용 {@code MANAGEMENT_SERVER_PORT=8081})는 별도 자식 컨텍스트로 뜨지만, Spring Boot의
     * {@code ServletManagementChildContextConfiguration}이 부모의 {@code springSecurityFilterChain}(FilterChainProxy)을 그 자식
     * 컨텍스트에 그대로 재등록한다 — 즉 관리 포트도 메인 포트와 동일한 이 클래스의 SecurityFilterChain 목록을 탄다. 아래 메인 체인은
     * {@code anyRequest().authenticated()}라 별도 permitAll이 없으면 {@code /actuator/prometheus}가 401이 되어 스크레이핑이 거부된다.
     * <p>
     * {@code EndpointRequest.toAnyEndpoint()}는 관리 포트가 분리되면 부모 컨텍스트에서 매칭되지 않으므로 쓰지 않고, 실제로 노출하는
     * 두 경로({@code MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,prometheus})만 경로로 열어 준다. 그 외 {@code /actuator/*}는
     * 이 체인에 매칭되지 않아 메인 체인으로 떨어져 401이 유지된다. 메인 포트(8080)에서는 관리 포트 분리로 actuator 핸들러가 아예
     * 없어 이 두 경로도 404이므로 실제 노출은 없다.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/prometheus", "/actuator/health")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Lazy JwtDecoder jwtDecoder,
            AuthenticationEntryPoint authenticationEntryPoint
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/docs/**", "/webjars/**", "/openapi.yaml", "/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(
                        exceptionHandling -> exceptionHandling.authenticationEntryPoint(authenticationEntryPoint))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));
        return http.build();
    }

    /**
     * issuer-uri는 실제 Cognito User Pool이 없으면 앱 기동 시점에 discovery 네트워크 호출이 실패한다. 빈 생성을 지연시켜, 인증이 필요한 요청이 실제로 들어오기
     * 전까지는(User Pool 준비 전까지는) 컨텍스트 기동이 막히지 않게 한다.
     */
    @Bean
    @Lazy
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${book-service.security.cognito.app-client-id}") String backendAppClientId
    ) {
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);
        decoder.setJwtValidator(new CognitoAccessTokenValidator(issuerUri, backendAppClientId));
        return decoder;
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return new JwtAuthenticationEntryPoint(objectMapper);
    }
}
