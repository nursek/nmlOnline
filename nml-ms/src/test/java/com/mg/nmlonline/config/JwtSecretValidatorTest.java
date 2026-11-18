package com.mg.nmlonline.config;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecretValidatorTest {

    @Test
    void rejectsPublishedSecretsAndAcceptsRandomOnes() {
        assertThrows(IllegalArgumentException.class, () -> new JwtSecretValidator(
                "dev-only-secret-key-at-least-32-characters-long-do-not-use-in-prod",
                "dev-only-pepper-secret-at-least-16-characters"));
        assertThrows(IllegalArgumentException.class, () -> new JwtSecretValidator(
                "test-secret-key-for-ci-at-least-32-chars-long",
                "test-pepper-value-for-ci-tests-only"));
        assertDoesNotThrow(() -> new JwtSecretValidator(
                UUID.randomUUID() + "-" + UUID.randomUUID(),
                UUID.randomUUID().toString()));
    }
}
