package com.chc.dpgb.security;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTest.PingController.class)
@Import({SecurityConfig.class, SecurityConfigTest.PingController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void 토큰_없이_요청하면_401과_통일된_에러_포맷을_반환한다() throws Exception {
        mockMvc.perform(get("/security-config-test/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    void 문서_경로는_토큰_없이_접근_가능하다() throws Exception {
        mockMvc.perform(get("/docs/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void actuator_prometheus와_health는_토큰_없이_통과한다() throws Exception {
        // 관리 포트(8081)도 이 SecurityFilterChain 목록을 타므로 permitAll이 없으면 401이 된다.
        // @WebMvcTest에는 actuator 엔드포인트가 없어 핸들러 부재로 401이 아닌 다른 상태가 나온다 —
        // 인증을 통과했다는 것만 검증한다(실제 8081에서는 200).
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().is(not(401)));
        mockMvc.perform(get("/actuator/health")).andExpect(status().is(not(401)));
    }

    @Test
    void 그_외_actuator_경로는_여전히_인증을_요구한다() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 유효한_토큰이면_sub_클레임에서_memberId를_추출한다() throws Exception {
        String memberId = "123e4567-e89b-12d3-a456-426614174000";
        mockMvc.perform(get("/security-config-test/ping")
                        .with(jwt().jwt(builder -> builder.subject(memberId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(memberId));
    }

    @RestController
    static class PingController {

        @GetMapping("/security-config-test/ping")
        public Map<String, String> ping(@AuthenticationPrincipal Jwt jwt) {
            return Map.of("memberId", MemberIdResolver.resolve(jwt).toString());
        }
    }
}
