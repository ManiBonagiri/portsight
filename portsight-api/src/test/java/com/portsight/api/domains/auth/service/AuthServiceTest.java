package com.portsight.api.domains.auth.service;

import com.portsight.api.domains.auth.User;
import com.portsight.api.domains.auth.UserRepository;
import com.portsight.api.domains.auth.UserRole;
import com.portsight.api.domains.auth.dto.JwtResponse;
import com.portsight.api.domains.auth.dto.LoginRequest;
import com.portsight.api.domains.auth.dto.UserRegistrationRequest;
import com.portsight.api.domains.auth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenPrefix", "refreshToken:");
        ReflectionTestUtils.setField(authService, "refreshTokenTtlSeconds", 604800L);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private UserRegistrationRequest buildRegistrationRequest(String email, UserRole role) {
        UserRegistrationRequest req = new UserRegistrationRequest();
        req.setEmail(email);
        req.setPassword("SecurePass@123");
        req.setFirstName("Test");
        req.setLastName("User");
        req.setRole(role);
        return req;
    }

    private User buildUser(UserRole role) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("investor@portsight.com")
                .passwordHash("$2a$12$hashedpassword")
                .firstName("Test")
                .lastName("User")
                .role(role)
                .status("ACTIVE")
                .build();
    }

    // ─── Registration Tests ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Registration")
    class RegistrationTests {

        @Test
        @DisplayName("Should register a new user successfully with ROLE_USER")
        void shouldRegisterUserSuccessfully() {
            UserRegistrationRequest request = buildRegistrationRequest("newuser@portsight.com", UserRole.ROLE_USER);

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(request.getPassword())).thenReturn("$2a$12$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authService.register(request, false);

            assertThat(result.getEmail()).isEqualTo("newuser@portsight.com");
            assertThat(result.getRole()).isEqualTo(UserRole.ROLE_USER);
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            assertThat(result.getPasswordHash()).isEqualTo("$2a$12$encoded");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email is already in use")
        void shouldThrowWhenEmailAlreadyExists() {
            UserRegistrationRequest request = buildRegistrationRequest("existing@portsight.com", UserRole.ROLE_USER);

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email already in use");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should force ROLE_USER when non-admin tries to register with elevated role")
        void shouldForceRoleUserForNonAdminRegistration() {
            UserRegistrationRequest request = buildRegistrationRequest("user@portsight.com", UserRole.ROLE_ADMIN);

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authService.register(request, false);

            // Role must be downgraded to ROLE_USER
            assertThat(result.getRole()).isEqualTo(UserRole.ROLE_USER);
        }

        @Test
        @DisplayName("Should allow admin to register a user with ROLE_ADMIN")
        void shouldAllowAdminToSetElevatedRole() {
            UserRegistrationRequest request = buildRegistrationRequest("admin@portsight.com", UserRole.ROLE_ADMIN);

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authService.register(request, true);

            assertThat(result.getRole()).isEqualTo(UserRole.ROLE_ADMIN);
        }

        @Test
        @DisplayName("Should encode password before saving")
        void shouldEncodePasswordBeforeSaving() {
            UserRegistrationRequest request = buildRegistrationRequest("user@portsight.com", UserRole.ROLE_USER);

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("SecurePass@123")).thenReturn("$2a$12$bcryptencoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authService.register(request, false);

            assertThat(result.getPasswordHash()).isEqualTo("$2a$12$bcryptencoded");
            assertThat(result.getPasswordHash()).doesNotContain("SecurePass@123");
            verify(passwordEncoder).encode("SecurePass@123");
        }
    }

    // ─── Login Tests ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully and return JWT tokens")
        void shouldLoginSuccessfully() {
            User user = buildUser(UserRole.ROLE_USER);
            LoginRequest request = new LoginRequest();
            request.setEmail("investor@portsight.com");
            request.setPassword("SecurePass@123");

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);
            when(jwtUtil.generateAccessToken(anyString(), anyString(), anyList())).thenReturn("mock.access.token");
            when(jwtUtil.generateRefreshToken(anyString())).thenReturn("mock.refresh.token");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            JwtResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("mock.access.token");
            assertThat(response.getRefreshToken()).isEqualTo("mock.refresh.token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            verify(valueOperations).set(eq("refreshToken:mock.refresh.token"), eq(user.getId().toString()), any());
        }

        @Test
        @DisplayName("Should throw exception when email does not exist")
        void shouldThrowWhenEmailNotFound() {
            LoginRequest request = new LoginRequest();
            request.setEmail("unknown@portsight.com");
            request.setPassword("anypassword");

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        @DisplayName("Should throw exception when password is incorrect")
        void shouldThrowWhenPasswordIncorrect() {
            User user = buildUser(UserRole.ROLE_USER);
            LoginRequest request = new LoginRequest();
            request.setEmail("investor@portsight.com");
            request.setPassword("WrongPassword");

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid credentials");

            // Tokens must never be generated on failed login
            verify(jwtUtil, never()).generateAccessToken(anyString(), anyString(), anyList());
            verify(jwtUtil, never()).generateRefreshToken(anyString());
        }

        @Test
        @DisplayName("Should store refresh token in Redis with correct TTL on login")
        void shouldStoreRefreshTokenInRedis() {
            User user = buildUser(UserRole.ROLE_USER);
            LoginRequest request = new LoginRequest();
            request.setEmail("investor@portsight.com");
            request.setPassword("SecurePass@123");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtUtil.generateAccessToken(anyString(), anyString(), anyList())).thenReturn("access.token");
            when(jwtUtil.generateRefreshToken(anyString())).thenReturn("refresh.token");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            authService.login(request);

            verify(valueOperations).set(
                    eq("refreshToken:refresh.token"),
                    eq(user.getId().toString()),
                    eq(java.time.Duration.ofSeconds(604800L)));
        }
    }

    // ─── Refresh Token Tests ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Token Refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should return new access token for valid refresh token")
        void shouldRefreshTokenSuccessfully() {
            User user = buildUser(UserRole.ROLE_USER);
            String refreshToken = "valid.refresh.token";

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("refreshToken:" + refreshToken)).thenReturn(user.getId().toString());
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(jwtUtil.generateAccessToken(anyString(), anyString(), anyList())).thenReturn("new.access.token");

            JwtResponse response = authService.refresh(refreshToken);

            assertThat(response.getAccessToken()).isEqualTo("new.access.token");
            assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
            assertThat(response.getTokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("Should throw exception when refresh token is invalid or expired")
        void shouldThrowWhenRefreshTokenInvalid() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("refreshToken:expired.token")).thenReturn(null);

            assertThatThrownBy(() -> authService.refresh("expired.token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Refresh token is invalid or expired");
        }

        @Test
        @DisplayName("Should throw exception when user not found during refresh")
        void shouldThrowWhenUserNotFoundDuringRefresh() {
            UUID randomId = UUID.randomUUID();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(randomId.toString());
            when(userRepository.findById(randomId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("some.refresh.token"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found");
        }
    }

    // ─── Logout Tests ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("Should delete refresh token from Redis on logout")
        void shouldDeleteRefreshTokenOnLogout() {
            String refreshToken = "valid.refresh.token";
            String bearerHeader = "Bearer " + refreshToken;

            authService.logout(bearerHeader);

            verify(redisTemplate).delete("refreshToken:" + refreshToken);
        }

        @Test
        @DisplayName("Should do nothing when Authorization header is null")
        void shouldDoNothingWhenHeaderIsNull() {
            authService.logout(null);

            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("Should do nothing when Authorization header does not start with Bearer")
        void shouldDoNothingWhenHeaderIsInvalid() {
            authService.logout("Basic sometoken");

            verify(redisTemplate, never()).delete(anyString());
        }
    }
}