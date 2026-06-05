package com.portsight.api.domains.auth.dto;

import com.portsight.api.domains.auth.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for user registration. Role is optional – defaults to USER if omitted.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String firstName;
    private String lastName;

    /**
     * Optional role – the service will enforce that only admins may assign ANALYST or ADMIN.
     */
    private UserRole role = UserRole.ROLE_USER;
}
