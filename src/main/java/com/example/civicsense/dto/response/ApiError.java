package com.example.civicsense.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Standardised error response envelope.
 * All REST errors are returned in this shape to simplify client-side error handling.
 *
 * Example JSON:
 * <pre>
 * {
 *   "timestamp": "2024-10-01T10:15:30Z",
 *   "status": 400,
 *   "error": "Validation Failed",
 *   "message": "Request validation errors",
 *   "fieldErrors": ["title: must not be blank"]
 * }
 * </pre>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    /** Per-field validation errors; present only on 400 validation failures. */
    private List<String> fieldErrors;
}
