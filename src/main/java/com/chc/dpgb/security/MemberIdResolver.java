package com.chc.dpgb.security;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

public final class MemberIdResolver {

    private MemberIdResolver() {
    }

    public static UUID resolve(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
