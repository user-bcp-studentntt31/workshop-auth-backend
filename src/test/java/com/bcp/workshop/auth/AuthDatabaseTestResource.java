package com.bcp.workshop.auth;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class AuthDatabaseTestResource implements QuarkusTestResourceLifecycleManager {
    static final String TEST_TOKEN_SECRET = "test-only-auth-token-secret-with-at-least-32-bytes";
    private Path databaseFile;

    @Override
    public Map<String, String> start() {
        try {
            databaseFile = Files.createTempFile("workshop-auth-test-", ".db");
                return Map.of(
                    "auth.db.url", "jdbc:sqlite:" + databaseFile.toAbsolutePath(),
                    "auth.token.secret", TEST_TOKEN_SECRET
                );
        } catch (IOException error) {
            throw new IllegalStateException("Could not create the authentication test database", error);
        }
    }

    @Override
    public void stop() {
        if (databaseFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(databaseFile);
        } catch (IOException error) {
            throw new IllegalStateException("Could not delete the authentication test database", error);
        }
    }
}
