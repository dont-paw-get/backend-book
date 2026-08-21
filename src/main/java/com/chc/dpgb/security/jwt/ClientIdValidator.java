package com.chc.dpgb.security.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Cognito Access Token의 {@code client_id} 클레임이 Book Service가 신뢰하는 App Client(웹앱)가 발급한 토큰인지 검증한다. {@code aud}가 없는
 * Cognito에서 표준 audience 검증을 대체하는 역할이다.
 */
public class ClientIdValidator implements OAuth2TokenValidator<Jwt> {

    private static final String CLIENT_ID_CLAIM = "client_id";

    private final String expectedClientId;

    public ClientIdValidator(String expectedClientId) {
        this.expectedClientId = expectedClientId;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (expectedClientId.equals(token.getClaimAsString(CLIENT_ID_CLAIM))) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "허용되지 않은 클라이언트가 발급한 토큰입니다.",
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}
