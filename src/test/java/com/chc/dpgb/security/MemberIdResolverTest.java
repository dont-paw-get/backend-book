package com.chc.dpgb.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class MemberIdResolverTest {

	@Test
	void sub_클레임을_memberId로_추출한다() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.claims(claims -> claims.putAll(Map.of("sub", "cognito-sub-uuid")))
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();

		String memberId = MemberIdResolver.resolve(jwt);

		assertThat(memberId).isEqualTo("cognito-sub-uuid");
	}
}
