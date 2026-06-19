package com.stationery.auth.controller;

import com.stationery.auth.dto.AuthResponse;
import com.stationery.auth.dto.LoginRequest;
import com.stationery.auth.dto.RegisterRequest;
import com.stationery.auth.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// REST controller for authentication - register, login, validate tokens.
// Separated from main app so auth can be scaled or swapped out independently.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    // Constructor injection - Spring auto-wires the service
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Register new user. @Valid checks constraints on the DTO before method executes.
    // Returns 201 Created on success with auth token.
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Registration request received for username: {}", request.getUsername());
        AuthResponse response = authService.register(request);
        logger.info("Registration successful for username: {}", request.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Login - authenticate user and return JWT. Service layer handles password verification and token generation.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request received for username: {}", request.getUsername());
        AuthResponse response = authService.login(request);
        logger.info("Login successful for username: {}", request.getUsername());
        return ResponseEntity.ok(response);
    }

    // Validate JWT token - called by API Gateway and other services for token verification.
    // Manually parse Bearer token from Authorization header.
    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String authHeader) {
        logger.info("Token validation request received");

        // Check header format before parsing - prevent NullPointerException or StringIndexOutOfBounds
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Token validation failed - invalid Authorization header format");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token format");
        }

        String token = authHeader.substring(7);
        boolean isValid = authService.validateToken(token);

        if (isValid) {
            logger.info("Token validation successful");
            return ResponseEntity.ok("Token is valid");
        } else {
            logger.warn("Token validation failed - token is invalid or expired");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired");
        }
    }
}
