package com.example.civicsense.storage;

import com.example.civicsense.dto.response.PresignedUrlResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Storage abstraction defining the contract for file storage and presigned upload operations.
 * Two implementations are provided:
 * <ul>
 *   <li>{@link LocalStorageService}  — stores files on disk; for development/testing.</li>
 *   <li>{@link S3StorageService}     — presigned URLs and uploads on AWS S3; for production.</li>
 * </ul>
 * The active implementation is selected via the {@code app.storage.use-local} property.
 */
public interface StorageService {

    /**
     * Generates a time-limited presigned PUT URL allowing the client to upload
     * an image directly to storage (S3) without proxying through Spring Boot.
     *
     * @param folder      target folder namespace ("before" or "after")
     * @param contentType MIME type of the file (e.g. "image/jpeg")
     * @param filename    original file name (used to retain extension)
     * @return {@link PresignedUrlResponse} containing uploadUrl and permanent fileUrl
     */
    PresignedUrlResponse generatePresignedUploadUrl(String folder, String contentType, String filename);

    /**
     * Direct upload fallback (accepts MultipartFile and returns the stored public URL).
     *
     * @param file   the multipart file uploaded by the client
     * @param folder logical folder/prefix (e.g. "before", "after")
     * @return the public URL of the stored file
     */
    String store(MultipartFile file, String folder);
}
