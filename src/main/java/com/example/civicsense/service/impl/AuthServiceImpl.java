package com.example.civicsense.service.impl;

import com.example.civicsense.dto.request.AuthRequest;
import com.example.civicsense.dto.request.RegisterRequest;
import com.example.civicsense.dto.response.AuthResponse;
import com.example.civicsense.entity.User;
import com.example.civicsense.exception.DuplicateEmailException;
import com.example.civicsense.repository.UserRepository;
import com.example.civicsense.security.JwtService;
import com.example.civicsense.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Handles user registration and JWT-based login.
 *
 * <p>Registration flow:
 * <ol>
 *   <li>Check email uniqueness.</li>
 *   <li>BCrypt-hash the password.</li>
 *   <li>Persist the User entity.</li>
 *   <li>Generate and return a JWT.</li>
 * </ol>
 *
 * <p>Login flow:
 * <ol>
 *   <li>Delegate to Spring Security AuthenticationManager (validates credentials).</li>
 *   <li>Load the User entity.</li>
 *   <li>Generate and return a JWT with the role as an extra claim.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        userRepository.save(user);
        log.info("Registered new user: email={}, role={}", user.getEmail(), user.getRole());

        String token = jwtService.generateToken(Map.of("role", user.getRole().name()), user);
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        // This throws BadCredentialsException automatically if credentials are wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User vanished after auth — should never happen"));

        String token = jwtService.generateToken(Map.of("role", user.getRole().name()), user);
        log.info("User logged in: email={}", user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }
}
