package com.example.civicsense.dto.request;

import com.example.civicsense.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for {@code PUT /api/complaints/{id}/status}.
 * The "afterImage" file is handled separately as an optional {@code MultipartFile}.
 */
@Data
public class UpdateStatusRequest {

    @NotNull(message = "Target status is required")
    private ComplaintStatus status;
}
