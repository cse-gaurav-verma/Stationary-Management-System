package com.stationery.auth.service;

import com.stationery.auth.dto.AuthResponse;
import com.stationery.auth.dto.LoginRequest;
import com.stationery.auth.dto.RegisterRequest;
import com.stationery.auth.model.Role;
import com.stationery.auth.model.User;
import com.stationery.auth.repository.UserRepository;
import com.stationery.auth.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Central hub for auth logic: handles user registration, logging in, and token checks.
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    // Creates a new user. We make sure the username and email aren't already floating around in the DB.
    // If they're unique, we securely hash the password, persist the user, and immediately issue a JWT 
    // so they don't have to log in right after registering.
    public AuthResponse register(RegisterRequest request) {
        logger.info("Attempting to register user: {}", request.getUsername());

        // Quick sanity checks to prevent duplicates
        if (userRepository.existsByUsername(request.getUsername())) {
            logger.warn("Registration failed - username already exists: {}", request.getUsername());
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Fallback mechanism: if the client passes some garbage role, we gracefully degrade 
            // them to a generic STUDENT. Safer than throwing a 500 error here.
            logger.warn("Invalid role provided: {}, defaulting to STUDENT", request.getRole());
            role = Role.STUDENT;
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // Always hash before saving!
                .role(role)
                .build();

        userRepository.save(user);
        logger.info("User registered successfully: {}", user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .message("User registered successfully")
                .build();
    }

    // Takes the raw credentials, throws them at Spring Security's AuthenticationManager, and if 
    // it doesn't blow up with a BadCredentialsException, we generate and return a shiny new JWT.
    public AuthResponse login(LoginRequest request) {
        logger.info("Attempting login for user: {}", request.getUsername());

        // This call will magically handle password verification via our configured UserDetailsService
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        logger.info("User authenticated successfully: {}", request.getUsername());

        // We pull the user from the DB again to grab their role for the JWT claims
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUsername()));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .message("Login successful")
                .build();
    }

    // Simple wrapper around JwtUtil. Useful if we want to add any DB checks 
    // (like checking if the user was deleted/banned) before declaring a token valid.
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }
}
