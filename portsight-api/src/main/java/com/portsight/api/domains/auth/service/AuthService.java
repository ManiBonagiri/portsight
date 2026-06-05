package com.portsight.api.domains.auth.service;

import com.portsight.api.domains.auth.User;
import com.portsight.api.domains.auth.UserRepository;
import com.portsight.api.domains.auth.UserRole;
import com.portsight.api.domains.auth.dto.UserRegistrationRequest;
import com.portsight.api.domains.auth.dto.LoginRequest;
import com.portsight.api.domains.auth.dto.JwtResponse;
import com.portsight.api.domains.auth.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Core authentication business logic.
 *
 * - Registration validates uniqueness and stores a BCrypt‑hashed password.
 * - Login issues an access token (15 min) and a refresh token (7 days) stored in Redis.
 * - Refresh validates the Redis‑stored token and issues a new access token.
 * - Logout removes the refresh token from Redis, making it unusable.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    // Configurable Redis key prefix – keeps the namespace tidy.
    @Value("${app.auth.refresh-token.prefix:refreshToken:}")
    private String refreshTokenPrefix;

    @Value("${app.auth.refresh-token.ttl-seconds:604800}") // 7 days
    private long refreshTokenTtlSeconds;

    @Transactional
    public User register(@Valid UserRegistrationRequest request, boolean isAdminRequest) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        UserRole role = request.getRole();
        // Only admins may set roles other than USER.
        if (!isAdminRequest && role != UserRole.ROLE_USER) {
            role = UserRole.ROLE_USER;
        }
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(role)
                .status("ACTIVE")
                .build();
        return userRepository.save(user);
    }

    public JwtResponse login(@Valid LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String accessToken = jwtUtil.generateAccessToken(user.getId().toString(), user.getEmail(), List.of(user.getRole()));
        String refreshToken = jwtUtil.generateRefreshToken(user.getId().toString());
        // Store refresh token in Redis with TTL
        redisTemplate.opsForValue().set(refreshTokenPrefix + refreshToken, user.getId().toString(), Duration.ofSeconds(refreshTokenTtlSeconds));
        return new JwtResponse(accessToken, refreshToken, "Bearer");
    }

    public JwtResponse refresh(String refreshToken) {
        String key = refreshTokenPrefix + refreshToken;
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            throw new IllegalArgumentException("Refresh token is invalid or expired");
        }
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String newAccess = jwtUtil.generateAccessToken(user.getId().toString(), user.getEmail(), List.of(user.getRole()));
        return new JwtResponse(newAccess, refreshToken, "Bearer");
    }

    public void logout(String bearerHeader) {
        if (bearerHeader == null || !bearerHeader.startsWith("Bearer ")) {
            return; // nothing to do
        }
        String token = bearerHeader.substring(7);
        // If token is a refresh token, simply delete the key. If it's an access token we ignore.
        String redisKey = refreshTokenPrefix + token;
        redisTemplate.delete(redisKey);
    }
}
