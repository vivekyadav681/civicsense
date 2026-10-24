package com.example.civicsense.service;

import com.example.civicsense.dto.request.AuthRequest;
import com.example.civicsense.dto.request.RegisterRequest;
import com.example.civicsense.dto.response.AuthResponse;
import com.example.civicsense.dto.response.RegisterResponse;

/**
 * Defines authentication operations: registration and login.
 */
public interface AuthService {

    /**
     * Registers a new user (CITIZEN or WORKER) and returns a success confirmation.
     *
     * @param request user registration details
     * @return confirmation message and newly created userId
     * @throws com.example.civicsense.exception.DuplicateEmailException if email already exists
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * Authenticates a user by email/password and returns a JWT token.
     *
     * @param request login credentials
     * @return token and role
     * @throws org.springframework.security.authentication.BadCredentialsException on invalid credentials
     */
    AuthResponse login(AuthRequest request);
}
