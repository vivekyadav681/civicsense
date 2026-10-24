package com.example.civicsense.dto.response;

import com.example.civicsense.enums.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response returned after updating status via PUT /api/complaints/{id}/status.
 * Format: { "id": 1, "status": "RESOLVED", "afterImageUrl": "url", "message": "Status updated successfully" }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintStatusUpdatedResponse {
    private Long id;
    private ComplaintStatus status;
    private String afterImageUrl;
    private String message;
}
