package com.example.civicsense.dto.request;

import com.example.civicsense.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

/** DTO for {@code POST /api/auth/register}. */
@Data
public class RegisterRequest {

    @NotBlank @Size(min = 2, max = 120)
    private String name;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /** Defaults to CITIZEN if not supplied. */
    private Role role = Role.CITIZEN;
}
