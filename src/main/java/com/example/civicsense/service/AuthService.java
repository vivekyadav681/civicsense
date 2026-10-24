package com.example.civicsense.service;

import com.example.civicsense.dto.request.AuthRequest;
import com.example.civicsense.dto.request.RegisterRequest;
import com.example.civicsense.dto.response.AuthResponse;

/**
 * Defines authentication operations: registration and login.
 */
public interface AuthService {

    /**
     * Registers a new user and returns a JWT token.
     *
     * @throws com.example.civicsense.exception.DuplicateEmailException if email already exists
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user by email/password and returns a JWT token.
     *
     * @throws org.springframework.security.authentication.BadCredentialsException on invalid credentials
     */
    AuthResponse login(AuthRequest request);
}
