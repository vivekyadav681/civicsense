package com.example.civicsense.controller;

import com.example.civicsense.dto.request.AuthRequest;
import com.example.civicsense.dto.request.RegisterRequest;
import com.example.civicsense.dto.response.AuthResponse;
import com.example.civicsense.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * Registers a new user and returns a JWT token.
     *
     * @param request validated registration payload
     * @return {@code 201 Created} with JWT and user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Authenticates an existing user and returns a fresh JWT token.
     *
     * @param request email + password credentials
     * @return {@code 200 OK} with JWT and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
