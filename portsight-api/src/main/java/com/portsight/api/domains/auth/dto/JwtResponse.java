package com.portsight.api.domains.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response payload returned after a successful login or token refresh.
 *
 * Fields:
 * - accessToken  : short-lived JWT used for API authorization (default 15 min).
 * - refreshToken : long-lived opaque token stored in Redis (default 7 days).
 * - tokenType    : always "Bearer" – included for OAuth2-style consumers.
 */
@Getter
@AllArgsConstructor
public class JwtResponse {

    @JsonProperty("access_token")
    private final String accessToken;

    @JsonProperty("refresh_token")
    private final String refreshToken;

    @JsonProperty("token_type")
    private final String tokenType;
}
