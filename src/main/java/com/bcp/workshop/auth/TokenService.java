package com.bcp.workshop.auth;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.Startup;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Startup
@ApplicationScoped
public class TokenService {
    @ConfigProperty(name = "auth.token.secret")
    String configuredSecret;

    private SecretKey signingKey;

    @PostConstruct
    void initialize() {
        signingKey = new SecretKeySpec(requireSigningKey(configuredSecret), "HmacSHA256");
    }

    SecretKey signingKey() {
        return signingKey;
    }

    static byte[] requireSigningKey(String secret) {
        if (secret == null) {
            throw new IllegalStateException("AUTH_TOKEN_SECRET must contain at least 32 bytes");
        }

        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) {
            throw new IllegalStateException("AUTH_TOKEN_SECRET must contain at least 32 bytes");
        }
        return key;
    }
}