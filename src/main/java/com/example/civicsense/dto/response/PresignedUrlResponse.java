package com.example.civicsense.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response containing permission details to upload an image directly to S3.
 *
 * <p>Flow:
 * <ol>
 *   <li>Client issues HTTP PUT with raw binary to {@link #uploadUrl}.</li>
 *   <li>Client takes {@link #fileUrl} and sends it in the JSON body of
 *       {@code POST /api/complaints} or {@code PUT /api/complaints/{id}/status}.</li>
 * </ol>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {

    /**
     * Time-limited presigned S3 PUT URL to which the client uploads the raw image binary.
     */
    private String uploadUrl;

    /**
     * The permanent public URL of the uploaded image to be stored in the database.
     */
    private String fileUrl;

    /**
     * The S3 object key (e.g. "before/uuid.jpg").
     */
    private String objectKey;

    /**
     * Number of seconds before the upload URL expires.
     */
    private long expiresInSeconds;
}
