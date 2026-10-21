package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenServiceTest {
    @Test
    void rejectsMissingSigningSecret() {
        assertThrows(IllegalStateException.class, () -> TokenService.requireSigningKey(null));
    }

    @Test
    void rejectsShortSigningSecret() {
        assertThrows(IllegalStateException.class, () -> TokenService.requireSigningKey("too-short"));
    }

    @Test
    void acceptsSecretWithAtLeast256Bits() {
        String secret = AuthDatabaseTestResource.TEST_TOKEN_SECRET;

        assertArrayEquals(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), TokenService.requireSigningKey(secret));
    }
}