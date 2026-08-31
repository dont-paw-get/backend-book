package com.chc.dpgb.security.jwt;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

/**
 * backend-auth가 Cognito Backend App Client로 발급한 Access Token만 통과시키는 검증 기준을 한 곳에 모은다.
 *
 * <p>프론트엔드는 Cognito와 직접 로그인하지 않는다 — {@code POST /api/v1/auth/login} → backend-auth → Cognito Backend App
 * Client 순으로 발급된 Access Token이 그대로 Book Service에 전달된다. 따라서 신뢰 대상은 backend-auth가 쓰는 Backend App Client 하나뿐이다.
 *
 * <p>검증 항목: 서명(JWKS는 {@code JwtDecoder}가 담당), issuer, 만료, {@code token_use == access}(ID Token 거부),
 * {@code client_id} 일치. Cognito Access Token에는 {@code aud}가 없으므로 표준 audience 검증은 쓰지 않는다.
 */
public class CognitoAccessTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final OAuth2TokenValidator<Jwt> delegate;

    public CognitoAccessTokenValidator(String issuerUri, String backendAppClientId) {
        this.delegate = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                new TokenUseValidator(),
                new ClientIdValidator(backendAppClientId)
        );
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        return delegate.validate(token);
    }
}
