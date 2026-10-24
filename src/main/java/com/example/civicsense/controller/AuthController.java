package com.example.civicsense.controller;

import com.example.civicsense.dto.request.AuthRequest;
import com.example.civicsense.dto.request.RegisterRequest;
import com.example.civicsense.dto.response.AuthResponse;
import com.example.civicsense.dto.response.RegisterResponse;
import com.example.civicsense.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication controller.
 *
 * <p>Both endpoints are permitted without authentication in {@link com.example.civicsense.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user (CITIZEN or WORKER).
     *
     * @param request validated registration payload
     * @return 201 Created with message and userId
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Authenticates an existing user and returns a fresh JWT token with role.
     *
     * @param request email + password credentials
     * @return 200 OK with token and role
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
