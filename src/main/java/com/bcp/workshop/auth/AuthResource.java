package com.bcp.workshop.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.text.StringEscapeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    private static final Logger LOGGER = Logger.getLogger(AuthResource.class.getName());

    @Inject
    LegacyDatabase database;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            if (request == null) {
                return Response.status(400).entity(Map.of("message", "Request is null")).build();
            } else {
                if (request.username == null || request.username.trim().isEmpty()) {
                    return Response.status(400).entity(Map.of("message", "Username is required")).build();
                } else {
                    if (request.password == null || request.password.trim().isEmpty()) {
                        return Response.status(400).entity(Map.of("message", "Password is required")).build();
                    } else {
                        if (request.client == null || request.client.trim().isEmpty()) {
                            return Response.status(400).entity(Map.of("message", "Client is required")).build();
                        }
                        UserRecord user = database.findByCredentials(request.username, request.password);
                        if (user == null) {
                            return Response.status(401).entity(Map.of("message", "Invalid credentials")).build();
                        } else {
                            String token = LegacyTokenUtil.create(user.username(), user.role(), request.client.trim());
                            Map<String, Object> response = new HashMap<>();
                            response.put("token", token);
                            response.put("expiresIn", LegacyTokenUtil.expiresInSeconds());
                            response.put("user", Map.of(
                                    "username", user.username(),
                                    "displayName", StringEscapeUtils.escapeHtml4(user.displayName()),
                                    "role", user.role()
                            ));
                            return Response.ok(response).build();
                        }
                    }
                }
            }
        } catch (Exception error) {
            LOGGER.log(Level.SEVERE, "Authentication failed due to internal error", error);
            return Response.status(500).entity(Map.of("message", "Authentication service unavailable")).build();
        }
    }
}
