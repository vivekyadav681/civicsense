package com.example.civicsense.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** DTO for {@code POST /api/auth/login}. */
@Data
public class AuthRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;
}
