package com.mg.nmlonline;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.UUID;

// Extension auto-détectée (junit-platform.properties) : un secret versionné serait une clé valide pour forger un jeton ADMIN.
public class TestSecrets implements BeforeAllCallback {

    static {
        System.setProperty("JWT_SECRET", UUID.randomUUID() + "-" + UUID.randomUUID());
        System.setProperty("JWT_PEPPER", UUID.randomUUID().toString());
    }

    @Override
    public void beforeAll(ExtensionContext context) {
    }
}
