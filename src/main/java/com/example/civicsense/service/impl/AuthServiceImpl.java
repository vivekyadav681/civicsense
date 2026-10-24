package com.example.civicsense.service.impl;

import com.example.civicsense.dto.request.AuthRequest;
import com.example.civicsense.dto.request.RegisterRequest;
import com.example.civicsense.dto.response.AuthResponse;
import com.example.civicsense.dto.response.RegisterResponse;
import com.example.civicsense.entity.Department;
import com.example.civicsense.entity.User;
import com.example.civicsense.enums.Role;
import com.example.civicsense.exception.DuplicateEmailException;
import com.example.civicsense.repository.DepartmentRepository;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Handles user registration and JWT-based authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository        userRepository;
    private final DepartmentRepository  departmentRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        Department department = null;
        if (request.getRole() == Role.WORKER && request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .department(department)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user: id={}, email={}, role={}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        return RegisterResponse.builder()
                .message("User registered successfully")
                .userId(savedUser.getId())
                .build();
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        if (user.getDepartment() != null) {
            claims.put("deptId", user.getDepartment().getId());
        }

        String token = jwtService.generateToken(claims, user);
        log.info("User logged in: email={}, role={}", user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .email(user.getEmail())
                .build();
    }
}
