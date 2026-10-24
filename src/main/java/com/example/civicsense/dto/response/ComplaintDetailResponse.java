package com.example.civicsense.dto.response;

import com.example.civicsense.enums.ComplaintCategory;
import com.example.civicsense.enums.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Full detail response for GET /api/complaints/{id}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private String workerNotes;

    /** Nested department details matching { "name": "Public Works" } */
    private DepartmentInfo department;
    private String departmentName;

    private String reporterName;
    private String reporterEmail;
    private Long parentComplaintId;

    /** List of child/duplicate complaints grouped under this parent issue */
    private List<ComplaintSummaryResponse> duplicates;

    private Instant createdAt;
    private Instant updatedAt;

    public Double getLat() {
        return latitude;
    }

    public Double getLng() {
        return longitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentInfo {
        private Long id;
        private String name;
    }
}
