package com.portsight.api.domains.auth.util;

import com.portsight.api.domains.auth.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper for creating and validating JWTs used by the application.
 *
 * Uses JJWT 0.12.x API. Tokens are signed with HMAC-SHA (HS256).
 * The secret key is loaded from application properties (base64-encoded).
 *
 * Access tokens carry: userId (subject), email, authorities (roles).
 * Refresh tokens carry: userId (subject) only — kept minimal for security.
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${jwt.access-token-ttl-ms:900000}") long accessTtl,
                   @Value("${jwt.refresh-token-ttl-ms:604800000}") long refreshTtl) {
        // JJWT 0.12.x: hmacShaKeyFor returns SecretKey directly
        this.secretKey = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secret));
        this.accessTokenValidityMs = accessTtl;
        this.refreshTokenValidityMs = refreshTtl;
    }

    public String generateAccessToken(String userId, String email, List<UserRole> roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenValidityMs))
                .claim("email", email)
                .claim("authorities", roles.stream().map(Enum::name).collect(Collectors.toList()))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTokenValidityMs))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        return (List<String>) claims.get("authorities", List.class);
    }
}
