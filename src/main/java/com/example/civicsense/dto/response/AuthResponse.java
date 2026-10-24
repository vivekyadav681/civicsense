package com.example.civicsense.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload for POST /api/auth/login.
 * Format: { "token": "JWT String", "role": "String" }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String token;
    private String role;
    private String email;

    public AuthResponse(String token, String role) {
        this.token = token;
        this.role = role;
    }
}
