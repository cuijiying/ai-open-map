package com.aomap.common.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public final class AuthTokens {

    public static final Duration TTL = Duration.ofHours(12);

    private AuthTokens() {
    }

    public static String sign(String secret, AuthToken token) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(Long.toString(token.userId()))
                .claim("org", token.orgId())
                .claim("role", token.role())
                .claim("name", token.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TTL)))
                .signWith(key(secret))
                .compact();
    }

    public static AuthToken parse(String secret, String jwt) {
        var claims = Jwts.parser().verifyWith(key(secret)).build().parseSignedClaims(jwt).getPayload();
        return new AuthToken(
                Long.parseLong(claims.getSubject()),
                claims.get("org", Number.class).longValue(),
                claims.get("role", String.class),
                claims.get("name", String.class)
        );
    }

    private static SecretKey key(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("登录密钥至少 32 个字符");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
