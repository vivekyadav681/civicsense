package com.example.civicsense.dto.response;

import com.example.civicsense.enums.ComplaintCategory;
import com.example.civicsense.enums.ComplaintStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Full response for {@code GET /api/complaints/{id}}.
 * Includes description, image URLs, reporter info, and the list of duplicate complaints.
 */
@Data
public class ComplaintDetailResponse {
    private Long id;
    private String title;
    private String description;
    private ComplaintCategory category;
    private ComplaintStatus status;
    private Double latitude;
    private Double longitude;
    private String beforeImageUrl;
    private String afterImageUrl;
    private String departmentName;
    private String reporterName;
    private String reporterEmail;
    private Long parentComplaintId;

    /** Summarised list of all duplicate complaints linked to this parent. */
    private List<ComplaintSummaryResponse> duplicates;

    private Instant createdAt;
    private Instant updatedAt;
}
