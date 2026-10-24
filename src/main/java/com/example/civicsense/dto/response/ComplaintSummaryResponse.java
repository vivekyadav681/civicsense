package com.example.civicsense.dto.response;

import com.example.civicsense.enums.ComplaintCategory;
import com.example.civicsense.enums.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Summary DTO used in paginated feeds and duplicate listings.
 * Compatible with both lat/lng and latitude/longitude consumers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintSummaryResponse {
    private Long id;
    private String title;
    private ComplaintCategory category;
    private ComplaintStatus status;
    private Double latitude;
    private Double longitude;
    private String beforeImageUrl;
    private String departmentName;
    private int duplicateCount;
    private Instant createdAt;

    public Double getLat() {
        return latitude;
    }

    public Double getLng() {
        return longitude;
    }
}
