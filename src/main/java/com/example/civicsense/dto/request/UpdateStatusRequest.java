package com.example.civicsense.dto.request;

import com.example.civicsense.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for {@code PUT /api/complaints/{id}/status}.
 * Accepts target status and optional direct S3 evidence URL via {@link #afterImageUrl}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "Target status is required")
    private ComplaintStatus status;

    /**
     * S3/public URL of the resolution evidence photo uploaded directly using the presigned URL.
     * Mandatory when transitioning to RESOLVED (unless provided via multipart file).
     */
    private String afterImageUrl;
}
