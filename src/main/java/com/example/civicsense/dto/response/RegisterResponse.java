package com.example.civicsense.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload for POST /api/auth/register.
 * Format: { "message": "User registered successfully", "userId": 1 }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private String message;
    private Long userId;
}
