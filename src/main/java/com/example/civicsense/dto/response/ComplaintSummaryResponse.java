package com.example.civicsense.dto.response;

import com.example.civicsense.enums.ComplaintCategory;
import com.example.civicsense.enums.ComplaintStatus;
import lombok.Data;

import java.time.Instant;

/**
 * Lightweight response for the paginated list endpoint.
 * Intentionally omits heavy fields (description, image URLs) to reduce payload size.
 */
@Data
public class ComplaintSummaryResponse {
    private Long id;
    private String title;
    private ComplaintCategory category;
    private ComplaintStatus status;
    private Double latitude;
    private Double longitude;
    private String departmentName;
    private int duplicateCount;
    private Instant createdAt;
}
