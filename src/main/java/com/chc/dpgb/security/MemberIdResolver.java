package com.chc.dpgb.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class MemberIdResolver {

    private MemberIdResolver() {
    }

    public static String resolve(Jwt jwt) {
        return jwt.getSubject();
    }
}
