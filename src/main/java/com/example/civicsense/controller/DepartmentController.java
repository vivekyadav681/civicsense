package com.example.civicsense.controller;

import com.example.civicsense.dto.response.DepartmentResponse;
import com.example.civicsense.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller exposing system configuration and municipal departments.
 */
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    /**
     * 9. Get List of Departments
     *
     * <p>Endpoint: GET /api/departments
     * <p>Role/Auth: Public (Used to populate frontend dropdowns and filters)
     * <p>Response: 200 OK with list of all civic departments
     */
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getDepartments() {
        List<DepartmentResponse> departments = departmentRepository.findAll().stream()
                .map(dept -> DepartmentResponse.builder()
                        .id(dept.getId())
                        .name(dept.getName())
                        .build())
                .toList();

        return ResponseEntity.ok(departments);
    }
}
