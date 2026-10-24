package com.example.civicsense.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.civicsense.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Cloudinary-backed implementation of {@link StorageService}.
 *
 * <p>Upload flow:
 * <ol>
 *   <li>Client sends a {@code multipart/form-data} request to Spring Boot.</li>
 *   <li>Spring Boot streams the file bytes directly to Cloudinary.</li>
 *   <li>Cloudinary returns a secure HTTPS URL stored in PostgreSQL.</li>
 * </ol>
 *
 * <p>Images are organised by folder: {@code <base-folder>/<sub-folder>/<original-filename>}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;

    /** Base folder configured via {@code app.storage.cloudinary.folder}. */
    @Value("${app.storage.cloudinary.folder:civicsense}")
    private String baseFolder;

    /**
     * Uploads {@code file} to Cloudinary under {@code <baseFolder>/<folder>}
     * and returns the secure delivery URL.
     *
     * @param file   multipart image received from the client
     * @param folder logical sub-folder ("before" or "after")
     * @return HTTPS Cloudinary URL of the uploaded image
     * @throws StorageException if the upload fails
     */
    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Cannot store an empty file.");
        }

        String publicIdFolder = baseFolder + "/" + folder;

        try {
            log.info("Uploading image to Cloudinary folder='{}', filename='{}'",
                    publicIdFolder, file.getOriginalFilename());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder",         publicIdFolder,
                            "resource_type",  "image",
                            "use_filename",   true,
                            "unique_filename", true
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            log.info("Cloudinary upload success. URL: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Cloudinary upload failed for file '{}': {}", file.getOriginalFilename(), e.getMessage());
            throw new StorageException("Failed to upload image to Cloudinary: " + e.getMessage(), e);
        }
    }
}
