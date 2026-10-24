package com.example.civicsense.storage;

import com.example.civicsense.dto.response.PresignedUrlResponse;
import com.example.civicsense.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * Production implementation of {@link StorageService} using AWS S3.
 *
 * <p>Supports:
 * <ul>
 *   <li><b>Presigned URLs</b>: Spring Boot generates a temporary, signed PUT URL
 *       allowing the client to upload images directly to AWS S3.</li>
 *   <li><b>Direct fallback</b>: Server-side upload via MultipartFile.</li>
 * </ul>
 */
@Service
@ConditionalOnProperty(name = "app.storage.use-local", havingValue = "false")
@RequiredArgsConstructor
@Slf4j
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.storage.s3.bucket-name}")
    private String bucketName;

    @Value("${app.storage.s3.base-url}")
    private String baseUrl;

    private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(15);

    @Override
    public PresignedUrlResponse generatePresignedUploadUrl(String folder, String contentType, String filename) {
        String extension = "";
        if (StringUtils.hasText(filename) && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf('.'));
        }

        // S3 object key: e.g. "before/45a8b79f-....jpg"
        String s3Key = folder + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(PRESIGNED_URL_DURATION)
                    .putObjectRequest(objectRequest)
                    .build();

            PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);
            String uploadUrl = presignedPutObjectRequest.url().toString();
            String fileUrl = baseUrl + "/" + s3Key;

            log.info("Generated S3 presigned PUT URL for key [{}], expires in {} min",
                    s3Key, PRESIGNED_URL_DURATION.toMinutes());

            return PresignedUrlResponse.builder()
                    .uploadUrl(uploadUrl)
                    .fileUrl(fileUrl)
                    .objectKey(s3Key)
                    .expiresInSeconds(PRESIGNED_URL_DURATION.toSeconds())
                    .build();

        } catch (Exception ex) {
            throw new StorageException("Failed to generate S3 presigned upload URL for key: " + s3Key, ex);
        }
    }

    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file");
        }

        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "upload";
        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : "";

        String s3Key = folder + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        } catch (IOException ex) {
            throw new StorageException("Failed to read file input stream for S3 upload", ex);
        } catch (Exception ex) {
            throw new StorageException("AWS S3 upload failed for key: " + s3Key, ex);
        }

        String fileUrl = baseUrl + "/" + s3Key;
        log.info("Uploaded to S3 → {}", fileUrl);
        return fileUrl;
    }
}
