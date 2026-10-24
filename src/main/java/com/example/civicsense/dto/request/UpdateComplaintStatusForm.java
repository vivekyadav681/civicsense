package com.example.civicsense.dto.request;

import com.example.civicsense.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * Multipart form data for PUT /api/complaints/{id}/status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateComplaintStatusForm {

    @NotNull(message = "status is required (IN_PROGRESS, RESOLVED, CLOSED)")
    private ComplaintStatus status;

    /** Required when status is RESOLVED. Optional for other statuses. */
    private MultipartFile afterImage;

    /** Optional notes/remarks from the field worker. */
    private String workerNotes;
}
