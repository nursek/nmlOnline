package com.mg.nmlonline.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class JwtSecretValidator {

    private static final Set<String> COMPROMISED = Set.of(
            "test-secret-key-for-ci-at-least-32-chars-long",
            "test-pepper-value-for-ci-tests-only");

    public JwtSecretValidator(@Value("${jwt.secret}") String secret,
                              @Value("${jwt.pepper}") String pepper) {
        validate(secret, 32, "jwt.secret");
        validate(pepper, 16, "jwt.pepper");
    }

    private static void validate(String value, int minLength, String name) {
        if (value == null || value.length() < minLength) {
            throw new IllegalArgumentException(name + " must be at least " + minLength + " characters long");
        }
        if (COMPROMISED.contains(value)) {
            throw new IllegalArgumentException(name + " uses a value published in this repository's history");
        }
    }
}
