package com.chc.dpgb.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class ClientIdValidatorTest {

    private final ClientIdValidator validator = new ClientIdValidator("expected-client-id");

    @Test
    void client_id가_일치하면_검증에_성공한다() {
        Jwt jwt = jwtWithClientId("expected-client-id");

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void client_id가_다르면_검증에_실패한다() {
        Jwt jwt = jwtWithClientId("other-client-id");

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt jwtWithClientId(String clientId) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claims(claims -> claims.putAll(Map.of("sub", "member-1", "client_id", clientId)))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
