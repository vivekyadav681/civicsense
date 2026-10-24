package com.example.civicsense.storage;

import com.example.civicsense.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * Production implementation of {@link StorageService}.
 * Uploads files to an AWS S3 bucket and returns a public HTTPS URL.
 * <br>
 * Activated when {@code app.storage.use-local=false}.
 *
 * <p><b>IAM permissions required:</b> {@code s3:PutObject} on the target bucket.</p>
 */
@Service
@ConditionalOnProperty(name = "app.storage.use-local", havingValue = "false")
@RequiredArgsConstructor
@Slf4j
public class S3StorageService implements StorageService {

    private final S3Client s3Client;

    @Value("${app.storage.s3.bucket-name}")
    private String bucketName;

    @Value("${app.storage.s3.base-url}")
    private String baseUrl;

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

        // S3 key: "before/uuid.jpg" or "after/uuid.jpg"
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
