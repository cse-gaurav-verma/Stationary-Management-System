package com.stationery.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// This is the core security setup for our auth service.
// We're keeping it simple: no sessions, no CSRF (since it's a stateless API), and exposing just what we need.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // We define our HTTP security rules here. 
    // CSRF is disabled because we're relying on tokens, not cookies, making us immune to cross-site request forgery.
    // We leave the login/register endpoints (/api/auth/**) and health checks (/actuator/**) wide open.
    // Everything else gets locked down. Also, forcing stateless session creation ensures Spring won't accidentally spin up HTTP sessions.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    // Standard BCrypt encoder. It automatically handles salting, which is exactly what we want for storing user passwords safely.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Spring Security no longer exposes the AuthenticationManager as a bean by default.
    // We have to grab it manually from the config so we can inject it into our login flows later.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
