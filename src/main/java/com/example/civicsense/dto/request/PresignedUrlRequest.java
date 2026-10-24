package com.example.civicsense.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for requesting permission to upload an image directly to S3.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlRequest {

    /**
     * Folder/prefix under which the object will be stored: 'before' or 'after'.
     */
    @NotBlank(message = "Folder is required ('before' or 'after')")
    @Pattern(regexp = "^(before|after)$", message = "Folder must be either 'before' or 'after'")
    private String folder;

    /**
     * MIME type of the image to be uploaded (e.g. image/jpeg, image/png, image/webp).
     */
    @NotBlank(message = "Content-Type is required (e.g. image/jpeg, image/png)")
    private String contentType;

    /**
     * Optional original filename to preserve extension (e.g. "pothole.jpg").
     */
    private String filename;
}
