package com.chc.dpgb.security.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Cognito Access Token은 {@code aud} 클레임이 없어 표준 audience 검증을 쓸 수 없다. 대신 {@code token_use}로 Access Token만 허용하고 ID Token은
 * 거부한다.
 */
public class TokenUseValidator implements OAuth2TokenValidator<Jwt> {

    private static final String TOKEN_USE_CLAIM = "token_use";
    private static final String EXPECTED_TOKEN_USE = "access";

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (EXPECTED_TOKEN_USE.equals(token.getClaimAsString(TOKEN_USE_CLAIM))) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "access token만 허용됩니다.",
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}
