package com.example.civicsense.controller;

import com.example.civicsense.dto.request.CreateComplaintRequest;
import com.example.civicsense.dto.request.UpdateStatusRequest;
import com.example.civicsense.dto.response.ComplaintDetailResponse;
import com.example.civicsense.dto.response.ComplaintSummaryResponse;
import com.example.civicsense.enums.ComplaintCategory;
import com.example.civicsense.enums.ComplaintStatus;
import com.example.civicsense.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller exposing the Civic Complaint API.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code POST   /api/complaints}            — File a complaint via JSON (S3 Presigned flow) or Multipart</li>
 *   <li>{@code GET    /api/complaints}             — Paginated, filtered list</li>
 *   <li>{@code GET    /api/complaints/{id}}        — Full detail with duplicates</li>
 *   <li>{@code PUT    /api/complaints/{id}/status} — Update status via JSON (S3 Presigned flow) or Multipart</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@Slf4j
public class ComplaintController {

    private final ComplaintService complaintService;

    // =========================================================================
    // POST /api/complaints  — File a new complaint
    // =========================================================================

    /**
     * Primary Flow (S3 Presigned URL): Accepts JSON payload with {@code beforeImageUrl}.
     * Spring Boot does NOT process image bytes, only storing the S3 URL in PostgreSQL.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ComplaintDetailResponse> createComplaintJson(
            @RequestBody @Valid CreateComplaintRequest request,
            Authentication auth) {

        log.info("Creating complaint via JSON (S3 direct upload flow) from: {}", auth.getName());
        ComplaintDetailResponse response =
                complaintService.createComplaint(request, null, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fallback / Multipart Flow: Accepts multipart form with complaint JSON part + beforeImage file part.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComplaintDetailResponse> createComplaintMultipart(
            @RequestPart("complaint") @Valid CreateComplaintRequest request,
            @RequestPart(value = "beforeImage", required = false) MultipartFile beforeImage,
            Authentication auth) {

        log.info("Creating complaint via Multipart from: {}", auth.getName());
        ComplaintDetailResponse response =
                complaintService.createComplaint(request, beforeImage, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // GET /api/complaints  — Paginated, filtered list
    // =========================================================================

    @GetMapping
    public ResponseEntity<Page<ComplaintSummaryResponse>> getComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(required = false) Long deptId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        int cappedSize = Math.min(size, 50);

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction sortDir = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(sortDir, sortField));

        return ResponseEntity.ok(
                complaintService.getComplaints(status, category, deptId, pageable));
    }

    // =========================================================================
    // GET /api/complaints/{id}  — Full detail with duplicates
    // =========================================================================

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintDetailResponse> getComplaintById(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaintById(id));
    }

    // =========================================================================
    // PUT /api/complaints/{id}/status  — Update status (WORKER / ADMIN)
    // =========================================================================

    /**
     * Primary Flow (S3 Presigned URL): Accepts JSON payload with {@code afterImageUrl} for RESOLVED.
     */
    @PutMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ComplaintDetailResponse> updateStatusJson(
            @PathVariable Long id,
            @RequestBody @Valid UpdateStatusRequest request) {

        log.info("Status update via JSON for complaint id={}, status={}", id, request.getStatus());
        return ResponseEntity.ok(complaintService.updateStatus(id, request, null));
    }

    /**
     * Fallback / Multipart Flow: Accepts statusUpdate JSON part + optional afterImage file part.
     */
    @PutMapping(value = "/{id}/status", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComplaintDetailResponse> updateStatusMultipart(
            @PathVariable Long id,
            @RequestPart("statusUpdate") @Valid UpdateStatusRequest request,
            @RequestPart(value = "afterImage", required = false) MultipartFile afterImage) {

        log.info("Status update via Multipart for complaint id={}, status={}", id, request.getStatus());
        return ResponseEntity.ok(complaintService.updateStatus(id, request, afterImage));
    }
}
