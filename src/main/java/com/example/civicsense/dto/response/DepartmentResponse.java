package com.example.civicsense.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response item for GET /api/departments.
 * Format: { "id": 1, "name": "Sanitation" }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {
    private Long id;
    private String name;
}
