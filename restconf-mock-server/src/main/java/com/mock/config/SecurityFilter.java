package com.mock.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Lightweight Security Filter for HTTP Basic Authentication.
 * Uses constant-time comparison (MessageDigest.isEqual) to prevent timing attacks.
 */
@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Value("${restconf.security.username:admin}")
    private String expectedUsername;

    @Value("${restconf.security.password:admin}")
    private String expectedPassword;

    @Value("${restconf.security.enabled:true}")
    private boolean securityEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!securityEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            sendUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        try {
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            byte[] decoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decoded, StandardCharsets.UTF_8);
            String[] values = credentials.split(":", 2);

            if (values.length != 2) {
                sendUnauthorized(response, "Malformed credentials");
                return;
            }

            byte[] providedUser = values[0].getBytes(StandardCharsets.UTF_8);
            byte[] expectedUser = expectedUsername.getBytes(StandardCharsets.UTF_8);
            byte[] providedPass = values[1].getBytes(StandardCharsets.UTF_8);
            byte[] expectedPass = expectedPassword.getBytes(StandardCharsets.UTF_8);

            boolean userMatch = MessageDigest.isEqual(providedUser, expectedUser);
            boolean passMatch = MessageDigest.isEqual(providedPass, expectedPass);

            if (!userMatch || !passMatch) {
                sendUnauthorized(response, "Invalid credentials");
                return;
            }
        } catch (IllegalArgumentException e) {
            sendUnauthorized(response, "Invalid Base64 encoding");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"" + message + "\",\"status\":401}");
    }
}
