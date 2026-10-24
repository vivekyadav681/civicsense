package com.example.civicsense.controller;

import com.example.civicsense.dto.request.CreateComplaintForm;
import com.example.civicsense.dto.request.UpdateComplaintStatusForm;
import com.example.civicsense.dto.response.ComplaintCreatedResponse;
import com.example.civicsense.dto.response.ComplaintDetailResponse;
import com.example.civicsense.dto.response.ComplaintStatusUpdatedResponse;
import com.example.civicsense.dto.response.ComplaintSummaryResponse;
import com.example.civicsense.enums.ComplaintCategory;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing civic complaints.
 */
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@Slf4j
public class ComplaintController {

    private final ComplaintService complaintService;

    /**
     * 3. Submit a New Complaint
     *
     * <p>Endpoint: POST /api/complaints
     * <p>Role/Auth: CITIZEN
     * <p>Request: Multipart Form-Data (title, description, category, latitude, longitude, beforeImage)
     * <p>Response: 201 Created
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CITIZEN') or hasRole('ADMIN')")
    public ResponseEntity<ComplaintCreatedResponse> createComplaint(
            @ModelAttribute @Valid CreateComplaintForm form,
            Authentication authentication) {

        log.info("Citizen [{}] filing complaint: '{}'", authentication.getName(), form.getTitle());
        ComplaintCreatedResponse response = complaintService.createComplaint(form, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 4. Get All Public Complaints (Map/Feed View)
     *
     * <p>Endpoint: GET /api/complaints
     * <p>Role/Auth: Public (or CITIZEN)
     * <p>Request (Query Params): ?category=WASTE&status=OPEN&lat=28.6&lng=77.2&radius=5000&page=0&size=20
     * <p>Response: 200 OK
     */
    @GetMapping
    public ResponseEntity<Page<ComplaintSummaryResponse>> getComplaints(
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        int cappedSize = Math.min(size, 100);
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction sortDir = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(sortDir, sortField));
        return ResponseEntity.ok(complaintService.getComplaints(category, status, lat, lng, radius, pageable));
    }

    /**
     * 5. Get Complaint Details (with Duplicates)
     *
     * <p>Endpoint: GET /api/complaints/{id}
     * <p>Role/Auth: Public (or CITIZEN / WORKER)
     * <p>Request: Path Variable id
     * <p>Response: 200 OK
     */
    @GetMapping("/{id}")
    public ResponseEntity<ComplaintDetailResponse> getComplaintById(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaintById(id));
    }

    /**
     * 6. Get My Submitted Complaints
     *
     * <p>Endpoint: GET /api/complaints/me
     * <p>Role/Auth: CITIZEN
     * <p>Request: None (Extract user identity from JWT)
     * <p>Response: 200 OK
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CITIZEN') or hasRole('ADMIN')")
    public ResponseEntity<Page<ComplaintSummaryResponse>> getMyComplaints(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        int cappedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(complaintService.getMyComplaints(authentication.getName(), pageable));
    }

    /**
     * 8. Update Complaint Status & Resolve
     *
     * <p>Endpoint: PUT /api/complaints/{id}/status
     * <p>Role/Auth: WORKER
     * <p>Request: Multipart Form-Data (status, afterImage, workerNotes)
     * <p>Response: 200 OK
     */
    @PutMapping(value = "/{id}/status", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('WORKER') or hasRole('ADMIN')")
    public ResponseEntity<ComplaintStatusUpdatedResponse> updateStatus(
            @PathVariable Long id,
            @ModelAttribute @Valid UpdateComplaintStatusForm form,
            Authentication authentication) {

        log.info("Worker [{}] updating complaint #{} status to {}",
                authentication.getName(), id, form.getStatus());
        ComplaintStatusUpdatedResponse response =
                complaintService.updateStatus(id, form, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
