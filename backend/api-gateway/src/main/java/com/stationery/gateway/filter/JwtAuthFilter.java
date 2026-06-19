package com.stationery.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Acts as the front door for our microservices.
// We intercept incoming requests here, validate the JWT, and pack the user details into headers 
// so the downstream services don't have to worry about token parsing.
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USER_NAME = "X-User-Name";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    // Keep these paths open so users can actually log in and register
    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/validate"
    );

    @Value("${jwt.secret:stationeryManagementSecretKey2024StationeryApp}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Let public endpoints through without checking tokens
        if (isOpenEndpoint(path)) {
            logger.debug("Skipping JWT validation for open endpoint: {}", path);
            return chain.filter(exchange);
        }

        // If there's no auth header, they aren't logged in at all
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            logger.warn("Missing Authorization header for path: {}", path);
            return onUnauthorized(exchange, "Missing Authorization header");
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.warn("Invalid Authorization header format for path: {}", path);
            return onUnauthorized(exchange, "Invalid Authorization header format");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Claims claims = extractClaims(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            if (username == null || username.isBlank()) {
                logger.warn("JWT token has no subject for path: {}", path);
                return onUnauthorized(exchange, "Invalid token: missing subject");
            }

            // Inject the user details into the headers. 
            // This is how downstream services know who's making the request without needing the JWT secret.
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(HEADER_USER_NAME, username)
                    .header(HEADER_USER_ROLE, role != null ? role : "ROLE_USER")
                    .build();

            logger.debug("JWT validated for user '{}' with role '{}' on path: {}",
                    username, role, path);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            logger.error("JWT validation failed for path {}: {}", path, e.getMessage());
            return onUnauthorized(exchange, "Invalid or expired token");
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    // Quick helper to check if a path is open to the public
    private boolean isOpenEndpoint(String path) {
        return OPEN_API_ENDPOINTS.stream()
                .anyMatch(endpoint -> path.startsWith(endpoint.replace("/**", "")));
    }

    // Parses the token using our secret. If it's expired or tampered with, this throws an exception
    // which gets caught in the filter method above.
    private Claims extractClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Fails the request with a 401 when the token is missing or busted
    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        logger.debug("Returning 401 Unauthorized: {}", message);
        return response.setComplete();
    }
}
