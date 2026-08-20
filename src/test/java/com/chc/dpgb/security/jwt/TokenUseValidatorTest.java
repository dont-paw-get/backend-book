package com.chc.dpgb.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class TokenUseValidatorTest {

	private final TokenUseValidator validator = new TokenUseValidator();

	@Test
	void token_use가_access이면_검증에_성공한다() {
		Jwt jwt = jwtWithTokenUse("access");

		OAuth2TokenValidatorResult result = validator.validate(jwt);

		assertThat(result.hasErrors()).isFalse();
	}

	@Test
	void token_use가_id이면_검증에_실패한다() {
		Jwt jwt = jwtWithTokenUse("id");

		OAuth2TokenValidatorResult result = validator.validate(jwt);

		assertThat(result.hasErrors()).isTrue();
	}

	@Test
	void token_use_클레임이_없으면_검증에_실패한다() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.claim("sub", "member-1")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();

		OAuth2TokenValidatorResult result = validator.validate(jwt);

		assertThat(result.hasErrors()).isTrue();
	}

	private Jwt jwtWithTokenUse(String tokenUse) {
		return Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.claims(claims -> claims.putAll(Map.of("sub", "member-1", "token_use", tokenUse)))
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
	}
}
