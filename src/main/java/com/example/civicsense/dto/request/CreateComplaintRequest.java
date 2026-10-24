package com.example.civicsense.dto.request;

import com.example.civicsense.enums.ComplaintCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO for {@code POST /api/complaints}.
 * The "beforeImage" file is handled separately as a {@code MultipartFile} in the controller.
 */
@Data
public class CreateComplaintRequest {

    @NotBlank(message = "Title must not be blank")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Description must not be blank")
    @Size(min = 10, message = "Description must be at least 10 characters")
    private String description;

    @NotNull(message = "Category is required")
    private ComplaintCategory category;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0",  message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0",   message = "Latitude must be <= 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0",  message = "Longitude must be <= 180")
    private Double longitude;
}
