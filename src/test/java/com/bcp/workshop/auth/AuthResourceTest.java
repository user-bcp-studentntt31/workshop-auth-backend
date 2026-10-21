package com.bcp.workshop.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(AuthDatabaseTestResource.class)
class AuthResourceTest {
    @Test
    void logsInWithWorkshopUser() throws Exception {
        assertSuccessfulLogin("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}", "angular-workshop");
    }

    @Test
    void rejectsInvalidCredentials() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"incorrect\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user student or password"));
    }

    @Test
    void rejectsNullRequest() {
        assertBadRequest("", "Request is null");
        assertBadRequest("null", "Request is null");
    }

    @Test
    void rejectsMissingOrBlankUsername() {
        List.of(
                "{\"password\":\"Workshop123!\"}",
                "{\"username\":null,\"password\":\"Workshop123!\"}",
                "{\"username\":\"\",\"password\":\"Workshop123!\"}",
                "{\"username\":\"   \",\"password\":\"Workshop123!\"}"
        ).forEach(body -> assertBadRequest(body, "Username is required"));
    }

    @Test
    void rejectsMissingOrBlankPassword() {
        List.of(
                "{\"username\":\"student\"}",
                "{\"username\":\"student\",\"password\":null}",
                "{\"username\":\"student\",\"password\":\"\"}",
                "{\"username\":\"student\",\"password\":\"   \"}"
        ).forEach(body -> assertBadRequest(body, "Password is required"));
    }

    @Test
    void rejectsMissingOrBlankClient() {
        assertBadRequest("{\"username\":\"student\",\"password\":\"Workshop123!\"}", "Client is required");
        assertBadRequest("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":null}", "Client is required");
        assertBadRequest("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"\"}", "Client is invalid");
        assertBadRequest("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"   \"}", "Client is invalid");
    }

    @Test
    void preservesClientExactlyInClaims() throws Exception {
        assertSuccessfulLogin("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop \"}", "angular-workshop ");
    }

    @Test
    void doesNotLogTokenOrSigningSecret() throws Exception {
        Logger logger = Logger.getLogger("com.bcp.workshop.auth");
        StringBuilder captured = new StringBuilder();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                captured.append(record.getMessage()).append('\n');
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        logger.addHandler(handler);
        try {
            String token = assertSuccessfulLogin(
                    "{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}",
                    "angular-workshop");
            given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"incorrect\",\"client\":\"angular-workshop\"}")
                    .when().post("/auth/login")
                    .then().statusCode(401);

            assertFalse(captured.toString().contains(token));
            assertFalse(captured.toString().contains("AUTH_TOKEN_SECRET"));
            assertFalse(captured.toString().contains(AuthDatabaseTestResource.TEST_TOKEN_SECRET));
        } finally {
            logger.removeHandler(handler);
        }
    }

    private static String assertSuccessfulLogin(String body, String expectedClient) throws Exception {
        Response response = given()
                .contentType("application/json")
                .body(body)
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("expiresIn", is(3600))
                .body("user.username", is("student"))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("debug", is("authenticated-with-legacy-flow"))
                .extract().response();

        String token = response.jsonPath().getString("token");
        SignedJWT signedJwt = SignedJWT.parse(token);
        assertEquals(JWSAlgorithm.HS256, signedJwt.getHeader().getAlgorithm());
        assertTrue(signedJwt.verify(new MACVerifier(AuthDatabaseTestResource.TEST_TOKEN_SECRET.getBytes(StandardCharsets.UTF_8))));

        JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
        assertEquals("student", claims.getSubject());
        assertEquals(expectedClient, claims.getStringClaim("client"));
        assertNotNull(claims.getIssueTime());
        assertNotNull(claims.getExpirationTime());

        long expiresIn = response.jsonPath().getLong("expiresIn");
        long secondsUntilExpiry = claims.getExpirationTime().toInstant().getEpochSecond() - Instant.now().getEpochSecond();
        assertTrue(Math.abs(expiresIn - secondsUntilExpiry) <= 5);
        return token;
    }

    private static void assertBadRequest(String body, String message) {
        given()
                .contentType("application/json")
                .body(body)
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is(message));
    }
}
