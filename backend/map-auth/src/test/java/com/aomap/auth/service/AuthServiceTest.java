package com.aomap.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    @Test
    void normalizesEmail() {
        assertEquals("user@example.com", AuthService.normalizeEmail(" User@Example.com "));
    }

    @Test
    void rejectsABadEmailOrShortPassword() {
        assertThrows(IllegalArgumentException.class, () -> AuthService.normalizeEmail("not-an-email"));
        assertThrows(IllegalArgumentException.class, () -> AuthService.checkPassword("short"));
    }
}
