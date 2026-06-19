package com.stationery.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// This acts as our central authority for JWT operations, keeping token management isolated
// from the rest of the application. We're going with a symmetric HMAC approach here, 
// meaning this service (and potentially any service validating tokens) relies on a shared secret.
// It's simpler than setting up asymmetric RSA keypairs, though it means we have to securely 
// distribute the secret if we move to a distributed validation model.
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    // We use JJWT's modern API which enforces strong typing and proper key sizing.
    // Instead of just passing around a string or raw bytes, we convert our secret into 
    // a proper cryptographic SecretKey instance. If the secret isn't long enough for HMAC-SHA, 
    // this will actually throw an error on startup, keeping us secure by default.
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // We build the token statelessly. Notice how we embed the user's role directly into the claims?
    // This is a deliberate design choice so downstream services or filters can authorize the user 
    // without having to do a round-trip to the database on every single request.
    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    // Quick helpers to grab what we need from the payload. Once the token is verified,
    // we can trust these values to populate our SecurityContext.
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // This is our main gatekeeper. JJWT's parser throws specific exceptions if the token 
    // fails verification (tampered, expired, malformed). We catch them explicitly here so we 
    // can log the exact security event without crashing the thread or returning messy 500 errors 
    // to the client. If it returns true, we know the token is cryptographically sound.
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    // Under the hood, this does the heavy lifting. The parser doesn't just read the claims; 
    // it verifies the signature against our SecretKey first. If someone tampered with the payload,
    // this will fail before we ever get the chance to read the manipulated data.
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
