package com.example.civicsense.dto.response;

import com.example.civicsense.enums.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /api/complaints.
 * Format: { "id": 1, "status": "REPORTED", "departmentId": 2, "parentComplaintId": null, "message": "Complaint recorded." }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintCreatedResponse {
    private Long id;
    private ComplaintStatus status;
    private Long departmentId;
    private Long parentComplaintId;
    private String message;
}
