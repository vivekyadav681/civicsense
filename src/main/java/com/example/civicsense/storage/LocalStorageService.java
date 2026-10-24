package com.example.civicsense.storage;

import com.example.civicsense.exception.StorageException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Development/test implementation of {@link StorageService}.
 * Stores uploaded files on the local filesystem under {@code app.storage.local-upload-dir}.
 * <br>
 * Activated when {@code app.storage.use-local=true} (the default).
 */
@Service
@ConditionalOnProperty(name = "app.storage.use-local", havingValue = "true", matchIfMissing = true)
@Slf4j
public class LocalStorageService implements StorageService {

    @Value("${app.storage.local-upload-dir:./uploads}")
    private String uploadDir;

    @Value("${server.port:8080}")
    private int serverPort;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("Local storage initialised at: {}", rootLocation);
        } catch (IOException ex) {
            throw new StorageException("Could not initialise local storage directory", ex);
        }
    }

    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload");

        // Guard against path-traversal attacks
        if (originalFilename.contains("..")) {
            throw new StorageException("Filename contains invalid path sequence: " + originalFilename, null);
        }

        // Generate a unique filename to avoid collisions
        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : "";
        String filename = folder + "/" + UUID.randomUUID() + extension;

        Path targetPath = rootLocation.resolve(filename);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new StorageException("Failed to store file: " + filename, ex);
        }

        // Return a localhost URL pointing to the static resource endpoint
        String fileUrl = "http://localhost:" + serverPort + "/uploads/" + filename;
        log.debug("Stored file locally → {}", fileUrl);
        return fileUrl;
    }
}
