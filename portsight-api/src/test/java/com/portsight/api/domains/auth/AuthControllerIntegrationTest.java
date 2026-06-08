package com.portsight.api.domains.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portsight.api.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Map<String, String> buildRegisterRequest(String email) {
        return Map.of(
                "email", email,
                "password", "SecurePass@123",
                "firstName", "Test",
                "lastName", "User");
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", password))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("access_token").asText();
    }

    private String loginAndGetRefreshToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", password))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("refresh_token").asText();
    }

    // ─── Registration Tests ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register a new user and return 201 with id, email, role")
        void shouldRegisterUserSuccessfully() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildRegisterRequest("newuser@portsight.com"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("newuser@portsight.com"))
                    .andExpect(jsonPath("$.role").value("ROLE_USER"))
                    .andExpect(jsonPath("$.id").isNotEmpty());
        }

        @Test
        @DisplayName("Should return 400 when email is already in use")
        void shouldReturn400WhenEmailAlreadyExists() throws Exception {
            // Register first time
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildRegisterRequest("duplicate@portsight.com"))))
                    .andExpect(status().isCreated());

            // Register again with same email
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildRegisterRequest("duplicate@portsight.com"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when required fields are missing")
        void shouldReturn400WhenFieldsMissing() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\": \"incomplete@portsight.com\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "email", "not-an-email",
                            "password", "SecurePass@123",
                            "firstName", "Test",
                            "lastName", "User"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── Login Tests ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully and return access_token, refresh_token, token_type")
        void shouldLoginSuccessfully() throws Exception {
            // Register first
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildRegisterRequest("loginuser@portsight.com"))))
                    .andExpect(status().isCreated());

            // Then login
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "email", "loginuser@portsight.com",
                            "password", "SecurePass@123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                    .andExpect(jsonPath("$.token_type").value("Bearer"));
        }

        @Test
        @DisplayName("Should return 400 when email does not exist")
        void shouldReturn400WhenEmailNotFound() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "email", "nobody@portsight.com",
                            "password", "SomePassword@1"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when password is incorrect")
        void shouldReturn400WhenPasswordWrong() throws Exception {
            // Register first
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildRegisterRequest("wrongpass@portsight.com"))))
                    .andExpect(status().isCreated());

            // Login with wrong password
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "email", "wrongpass@portsight.com",
                            "password", "WrongPassword@1"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── Refresh Token Tests ──────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTests {

        @Test
        @DisplayName("Should return new access token for valid refresh token")
        void shouldRefreshTokenSuccessfully() throws Exception {
            // Register + login
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildRegisterRequest("refresh@portsight.com"))))
                    .andExpect(status().isCreated());

            String refreshToken = loginAndGetRefreshToken("refresh@portsight.com", "SecurePass@123");

            // Use refresh token
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.token_type").value("Bearer"));
        }

        @Test
        @DisplayName("Should return 400 for invalid or expired refresh token")
        void shouldReturn400ForInvalidRefreshToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("refreshToken", "invalid.token.here"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── Logout Tests ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully and return 204")
        void shouldLogoutSuccessfully() throws Exception {
            // Register + login
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildRegisterRequest("logout@portsight.com"))))
                    .andExpect(status().isCreated());

            String accessToken = loginAndGetAccessToken("logout@portsight.com", "SecurePass@123");

            // Logout
            mockMvc.perform(post("/api/v1/auth/logout")
                    .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 204 even when no Authorization header provided")
        void shouldReturn204WithNoAuthHeader() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should invalidate refresh token after logout")
        void shouldInvalidateRefreshTokenAfterLogout() throws Exception {
            // Register + login
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buildRegisterRequest("logoutrefresh@portsight.com"))))
                    .andExpect(status().isCreated());

            String accessToken = loginAndGetAccessToken("logoutrefresh@portsight.com", "SecurePass@123");
            String refreshToken = loginAndGetRefreshToken("logoutrefresh@portsight.com", "SecurePass@123");

            // Logout
            mockMvc.perform(post("/api/v1/auth/logout")
                    .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent());

            // Try to use the refresh token — should now fail
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                    .andExpect(status().isBadRequest());
        }
    }
}