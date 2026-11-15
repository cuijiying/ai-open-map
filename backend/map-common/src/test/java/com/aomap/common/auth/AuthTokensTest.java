package com.aomap.common.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthTokensTest {

    private static final String SECRET = "aomap-local-dev-secret-change-me";

    @Test
    void roundTrip() {
        String jwt = AuthTokens.sign(SECRET, new AuthToken(7, 3, "OWNER", "测试"));
        AuthToken token = AuthTokens.parse(SECRET, jwt);
        assertEquals(7, token.userId());
        assertEquals(3, token.orgId());
        assertEquals("OWNER", token.role());
        assertEquals("测试", token.name());
    }

    @Test
    void rejectsAShortSecret() {
        assertThrows(IllegalArgumentException.class, () -> AuthTokens.sign("short", new AuthToken(1, 1, "OWNER", "a")));
    }

    @Test
    void rejectsATamperedToken() {
        String jwt = AuthTokens.sign(SECRET, new AuthToken(1, 1, "OWNER", "a"));
        assertThrows(Exception.class, () -> AuthTokens.parse(SECRET + "x", jwt));
    }
}
