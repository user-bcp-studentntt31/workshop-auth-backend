package com.bcp.workshop.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public final class LegacyTokenUtil {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String DEFAULT_SECRET = "WORKSHOP_SECRET_2026";
    private static final long EXPIRATION_SECONDS = 3600;

    private LegacyTokenUtil() {}

    public static String create(String username, String role, String client) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiry = issuedAt + EXPIRATION_SECONDS;
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{\"sub\":\"" + escape(username) + "\",\"role\":\"" + escape(role) + "\",\"client\":\"" + escape(client) + "\",\"iat\":" + issuedAt + ",\"exp\":" + expiry + "}";
        String encodedHeader = URL_ENCODER.encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = URL_ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String content = encodedHeader + "." + encodedPayload;
        return content + "." + sign(content, resolveSecret());
    }

    public static long expiresInSeconds() {
        return EXPIRATION_SECONDS;
    }

    private static String resolveSecret() {
        String configured = System.getenv("AUTH_TOKEN_SECRET");
        if (configured == null || configured.trim().isEmpty()) {
            return DEFAULT_SECRET;
        }
        return configured;
    }

    private static String sign(String content, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signed = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(signed);
        } catch (Exception error) {
            throw new IllegalStateException("Token signing failed", error);
        }
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
