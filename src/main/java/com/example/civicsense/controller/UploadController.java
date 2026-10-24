package com.example.civicsense.controller;

import com.example.civicsense.dto.request.PresignedUrlRequest;
import com.example.civicsense.dto.response.PresignedUrlResponse;
import com.example.civicsense.storage.LocalStorageService;
import com.example.civicsense.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * Controller managing image upload authorization and presigned URLs.
 *
 * <p>Production S3 Flow:
 * <ol>
 *   <li>Client calls {@code POST /api/uploads/presigned-url} with folder & contentType.</li>
 *   <li>Spring Boot checks auth and generates a signed AWS S3 PUT URL.</li>
 *   <li>Client PUTs the raw file binary directly to S3 {@code uploadUrl}.</li>
 *   <li>Client sends the returned {@code fileUrl} in {@code POST /api/complaints} or
 *       {@code PUT /api/complaints/{id}/status}.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final StorageService storageService;
    private final ObjectProvider<LocalStorageService> localStorageServiceProvider;

    /**
     * Issues permission to upload an image directly to storage (S3) by generating
     * a time-limited presigned PUT URL.
     *
     * @param request folder ("before" or "after") and contentType
     * @return {@link PresignedUrlResponse} containing uploadUrl and final fileUrl
     */
    @PostMapping("/presigned-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PresignedUrlResponse> getPresignedUploadUrl(
            @RequestBody @Valid PresignedUrlRequest request) {

        log.info("Generating presigned upload URL: folder={}, contentType={}, filename={}",
                request.getFolder(), request.getContentType(), request.getFilename());

        PresignedUrlResponse response = storageService.generatePresignedUploadUrl(
                request.getFolder(),
                request.getContentType(),
                request.getFilename()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Mock S3 PUT endpoint active during local testing (when USE_LOCAL_STORAGE=true).
     * Allows front-end and cURL clients to perform an HTTP PUT with raw image binary
     * just like with AWS S3!
     */
    @PutMapping("/mock-s3/**")
    public ResponseEntity<Map<String, String>> handleMockS3Put(HttpServletRequest request) throws IOException {
        LocalStorageService localService = localStorageServiceProvider.getIfAvailable();
        if (localService == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String fullPath = request.getRequestURI();
        String prefix = "/api/uploads/mock-s3/";
        int idx = fullPath.indexOf(prefix);
        if (idx == -1) {
            return ResponseEntity.badRequest().build();
        }

        String key = fullPath.substring(idx + prefix.length());
        localService.storeRawStream(key, request.getInputStream());

        return ResponseEntity.ok(Map.of("message", "File uploaded successfully to local storage", "key", key));
    }
}
