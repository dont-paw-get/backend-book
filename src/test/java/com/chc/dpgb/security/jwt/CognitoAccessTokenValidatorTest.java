package com.chc.dpgb.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * {@code SecurityConfig}가 {@code JwtDecoder}에 설정하는 검증 기준을 실제로 서명된 토큰으로 검증한다. 로컬 RSA 키쌍으로 서명하므로 Cognito
 * JWKS/discovery 네트워크 호출 없이 결정론적으로 동작한다.
 */
class CognitoAccessTokenValidatorTest {

    private static final String ISSUER =
            "https://cognito-idp.ap-northeast-2.amazonaws.com/ap-northeast-2_testpool";
    private static final String BACKEND_CLIENT_ID = "backend-app-client-id";
    private static final String LEGACY_FRONTEND_CLIENT_ID = "legacy-frontend-app-client-id";
    private static final String MEMBER_ID = "123e4567-e89b-12d3-a456-426614174000";

    private static RSAPublicKey publicKey;
    private static RSAPrivateKey privateKey;
    private static RSAPrivateKey otherPrivateKey;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        otherPrivateKey = (RSAPrivateKey) generator.generateKeyPair().getPrivate();
    }

    @Test
    void backend_app_client의_access_token은_aud가_없어도_인증에_성공한다() {
        String token = accessToken(ISSUER, BACKEND_CLIENT_ID, "access", validExpiry(), privateKey);

        Jwt jwt = decoder().decode(token);

        assertThat(jwt.getSubject()).isEqualTo(MEMBER_ID);
        assertThat(jwt.getClaimAsString("client_id")).isEqualTo(BACKEND_CLIENT_ID);
        assertThat(jwt.getAudience()).isNullOrEmpty();
    }

    @Test
    void 예전_frontend_app_client의_access_token은_인증에_실패한다() {
        String token = accessToken(ISSUER, LEGACY_FRONTEND_CLIENT_ID, "access", validExpiry(), privateKey);

        assertThatThrownBy(() -> decoder().decode(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("허용되지 않은 클라이언트가 발급한 토큰입니다.");
    }

    @Test
    void token_use가_id면_client_id가_맞아도_인증에_실패한다() {
        String token = accessToken(ISSUER, BACKEND_CLIENT_ID, "id", validExpiry(), privateKey);

        assertThatThrownBy(() -> decoder().decode(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("access token만 허용됩니다.");
    }

    @Test
    void 다른_user_pool이_발급한_토큰은_인증에_실패한다() {
        String token = accessToken(
                "https://cognito-idp.ap-northeast-2.amazonaws.com/ap-northeast-2_otherpool",
                BACKEND_CLIENT_ID, "access", validExpiry(), privateKey);

        assertThatThrownBy(() -> decoder().decode(token))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void 만료된_access_token은_인증에_실패한다() {
        String token = accessToken(
                ISSUER, BACKEND_CLIENT_ID, "access", Instant.now().minusSeconds(3600), privateKey);

        assertThatThrownBy(() -> decoder().decode(token))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void 다른_키로_서명된_토큰은_인증에_실패한다() {
        String token = accessToken(ISSUER, BACKEND_CLIENT_ID, "access", validExpiry(), otherPrivateKey);

        assertThatThrownBy(() -> decoder().decode(token))
                .isInstanceOf(JwtException.class);
    }

    private NimbusJwtDecoder decoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        decoder.setJwtValidator(new CognitoAccessTokenValidator(ISSUER, BACKEND_CLIENT_ID));
        return decoder;
    }

    private Instant validExpiry() {
        return Instant.now().plusSeconds(300);
    }

    /**
     * Cognito Access Token과 같은 형태(= {@code aud} 클레임 없음)로 서명한다.
     */
    private String accessToken(String issuer, String clientId, String tokenUse, Instant expiresAt,
                               RSAPrivateKey signingKey) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(MEMBER_ID)
                .issuer(issuer)
                .claim("client_id", clientId)
                .claim("token_use", tokenUse)
                .issueTime(Date.from(expiresAt.minusSeconds(3600)))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        try {
            signedJwt.sign(new RSASSASigner(signingKey));
        } catch (JOSEException e) {
            throw new IllegalStateException("테스트 토큰 서명에 실패했습니다.", e);
        }
        return signedJwt.serialize();
    }
}
