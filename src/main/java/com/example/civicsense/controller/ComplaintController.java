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
 *   <li>{@code POST   /api/complaints}            — File a new complaint (CITIZEN)</li>
 *   <li>{@code GET    /api/complaints}             — Paginated, filtered list</li>
 *   <li>{@code GET    /api/complaints/{id}}        — Full detail with duplicates</li>
 *   <li>{@code PUT    /api/complaints/{id}/status} — Update status (WORKER/ADMIN)</li>
 * </ul>
 *
 * <h2>Multipart Handling</h2>
 * <p>Image uploads use {@code multipart/form-data}. Text fields are sent as form
 * parts and bound to the DTO via {@code @RequestPart}. The image files are bound
 * as {@code MultipartFile} parts.
 *
 * <h2>Role Security</h2>
 * <p>Fine-grained role checks are enforced at the service layer via {@code @PreAuthorize}
 * annotations. The controller layer only performs basic authentication checks.
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
     * Accepts a multipart form with complaint data and a required before-image.
     *
     * <p>Form parts:
     * <ul>
     *   <li>{@code complaint}   — JSON blob matching {@link CreateComplaintRequest}</li>
     *   <li>{@code beforeImage} — Image file (JPEG/PNG, max 10 MB)</li>
     * </ul>
     *
     * <p>Internally this endpoint:
     * <ol>
     *   <li>Validates the request body.</li>
     *   <li>Runs the deduplication engine (50 m Haversine query).</li>
     *   <li>Routes to the correct department.</li>
     *   <li>Uploads the before-image.</li>
     *   <li>Returns HTTP 201 with the created complaint detail.</li>
     * </ol>
     *
     * @param request     validated complaint data (JSON part)
     * @param beforeImage photo evidence of the issue (file part)
     * @param auth        Spring Security principal — provides reporter email
     * @return {@code 201 Created} with {@link ComplaintDetailResponse}
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComplaintDetailResponse> createComplaint(
            @RequestPart("complaint") @Valid CreateComplaintRequest request,
            @RequestPart(value = "beforeImage", required = false) MultipartFile beforeImage,
            Authentication auth) {

        String reporterEmail = auth.getName();  // email, as per UserDetails#getUsername()
        log.info("Complaint creation request from: {}", reporterEmail);

        ComplaintDetailResponse response =
                complaintService.createComplaint(request, beforeImage, reporterEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // GET /api/complaints  — Paginated, filtered list
    // =========================================================================

    /**
     * Returns a paginated list of top-level (non-duplicate) complaints.
     * All filter parameters are optional — omitting them returns all complaints.
     *
     * <p>Query parameters:
     * <ul>
     *   <li>{@code status}   — Filter by {@link ComplaintStatus} (e.g. REPORTED)</li>
     *   <li>{@code category} — Filter by {@link ComplaintCategory} (e.g. WASTE)</li>
     *   <li>{@code deptId}   — Filter by department ID</li>
     *   <li>{@code page}     — 0-indexed page number (default 0)</li>
     *   <li>{@code size}     — Page size (default 10, max 50)</li>
     *   <li>{@code sort}     — Sort field and direction (default: createdAt,desc)</li>
     * </ul>
     *
     * @return {@code 200 OK} with a {@link Page} of {@link ComplaintSummaryResponse}
     */
    @GetMapping
    public ResponseEntity<Page<ComplaintSummaryResponse>> getComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(required = false) Long deptId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        // Cap page size to prevent abuse
        int cappedSize = Math.min(size, 50);

        // Parse "field,direction" sort string (e.g. "createdAt,desc")
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

    /**
     * Returns the full detail of a single complaint, including its list of
     * duplicate (child) complaints and all image URLs.
     *
     * @param id the complaint ID
     * @return {@code 200 OK} with {@link ComplaintDetailResponse}
     * @throws com.example.civicsense.exception.ResourceNotFoundException if not found → 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<ComplaintDetailResponse> getComplaintById(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaintById(id));
    }

    // =========================================================================
    // PUT /api/complaints/{id}/status  — Update status (WORKER / ADMIN)
    // =========================================================================

    /**
     * Updates the status of a complaint, enforcing the state machine rules.
     *
     * <p>Form parts:
     * <ul>
     *   <li>{@code statusUpdate} — JSON blob matching {@link UpdateStatusRequest}</li>
     *   <li>{@code afterImage}   — Optional after-evidence photo; <b>required</b>
     *       when transitioning to {@code RESOLVED}</li>
     * </ul>
     *
     * <p>State machine rules:
     * <pre>
     *   REPORTED    → ASSIGNED
     *   ASSIGNED    → IN_PROGRESS
     *   IN_PROGRESS → RESOLVED  (afterImage required)
     *   RESOLVED    → CLOSED
     * </pre>
     *
     * @param id           the complaint ID
     * @param request      the desired new status (JSON part)
     * @param afterImage   resolution evidence photo (optional file part)
     * @return {@code 200 OK} with updated {@link ComplaintDetailResponse}
     * @throws com.example.civicsense.exception.IllegalStateTransitionException on invalid transition → 422
     */
    @PutMapping(value = "/{id}/status", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComplaintDetailResponse> updateStatus(
            @PathVariable Long id,
            @RequestPart("statusUpdate") @Valid UpdateStatusRequest request,
            @RequestPart(value = "afterImage", required = false) MultipartFile afterImage) {

        log.info("Status update request: complaintId={}, targetStatus={}", id, request.getStatus());
        return ResponseEntity.ok(complaintService.updateStatus(id, request, afterImage));
    }
}
