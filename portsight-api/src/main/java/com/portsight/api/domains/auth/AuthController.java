package com.portsight.api.domains.auth;

import com.portsight.api.domains.auth.dto.LoginRequest;
import com.portsight.api.domains.auth.dto.UserRegistrationRequest;
import com.portsight.api.domains.auth.dto.JwtResponse;
import com.portsight.api.domains.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public authentication endpoints.
 *
 * - {@code POST /api/v1/auth/register} – creates a new user. If the caller is an admin, they may assign
 *   a role other than {@code ROLE_USER}. Otherwise role is forced to {@code ROLE_USER}.
 * - {@code POST /api/v1/auth/login} – validates credentials and returns JWT access + refresh tokens.
 * - {@code POST /api/v1/auth/refresh} – exchanges a valid refresh token for a fresh access token.
 * - {@code POST /api/v1/auth/logout} – revokes the supplied refresh token (if any).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody UserRegistrationRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        // Service will enforce role constraints based on this flag.
        var user = authService.register(request, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse jwt = authService.login(request);
        return ResponseEntity.ok(jwt);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        JwtResponse jwt = authService.refresh(refreshToken);
        return ResponseEntity.ok(jwt);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(name = "Authorization", required = false) String bearer) {
        authService.logout(bearer);
        return ResponseEntity.noContent().build();
    }
}
