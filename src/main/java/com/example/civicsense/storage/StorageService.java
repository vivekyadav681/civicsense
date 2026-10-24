package com.example.civicsense.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Storage abstraction for image uploads.
 *
 * <p>The single production implementation is
 * {@link CloudinaryStorageService}, which uploads files directly to
 * Cloudinary and returns the secure HTTPS URL.
 */
public interface StorageService {

    /**
     * Uploads a multipart file to cloud storage and returns the permanent
     * public URL of the stored resource.
     *
     * @param file   the image file received from the client
     * @param folder logical folder/namespace (e.g. {@code "before"}, {@code "after"})
     * @return secure HTTPS URL of the uploaded image
     */
    String store(MultipartFile file, String folder);
}
