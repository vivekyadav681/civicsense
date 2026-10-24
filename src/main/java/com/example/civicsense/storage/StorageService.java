package com.example.civicsense.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Storage abstraction defining the contract for file upload operations.
 * Two implementations are provided:
 * <ul>
 *   <li>{@link LocalStorageService}  — stores files on disk; for development/testing.</li>
 *   <li>{@link S3StorageService}     — stores files on AWS S3; for production.</li>
 * </ul>
 * The active implementation is selected via the {@code app.storage.use-local} property.
 */
public interface StorageService {

    /**
     * Stores the given file and returns its publicly accessible URL.
     *
     * @param file   the multipart file uploaded by the client
     * @param folder logical folder/prefix (e.g. "before", "after") used for namespacing
     * @return the public URL of the stored file
     * @throws com.example.civicsense.exception.StorageException on any I/O or SDK failure
     */
    String store(MultipartFile file, String folder);
}
