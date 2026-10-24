package com.example.civicsense.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Returned after a successful login. */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String role;
}
