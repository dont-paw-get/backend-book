package com.chc.dpgb.security.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Cognito Access Token의 {@code client_id} 클레임이 backend-auth가 사용하는 Cognito Backend App Client가 발급한 토큰인지 검증한다.
 * {@code aud}가 없는 Cognito Access Token에서 표준 audience 검증을 대체하는 역할이다.
 *
 * <p>신뢰 대상은 이 App Client 하나뿐이다 — 폐기된 프론트엔드 전용 App Client 토큰은 거부한다(CLIAR-188).
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
