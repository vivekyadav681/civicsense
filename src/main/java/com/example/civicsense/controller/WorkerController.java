package com.example.civicsense.controller;

import com.example.civicsense.dto.request.UpdateComplaintStatusForm;
import com.example.civicsense.dto.response.ComplaintStatusUpdatedResponse;
import com.example.civicsense.dto.response.ComplaintSummaryResponse;
import com.example.civicsense.enums.ComplaintStatus;
import com.example.civicsense.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for municipal field workers and department staff.
 */
@RestController
@RequestMapping("/api/worker/complaints")
@RequiredArgsConstructor
@Slf4j
public class WorkerController {

    private final ComplaintService complaintService;

    /**
     * 7. Get Department Queue (Kanban Board)
     *
     * <p>Endpoint: GET /api/worker/complaints
     * <p>Role/Auth: WORKER
     * <p>Request (Query Params): ?status=ASSIGNED&page=0&size=20
     * <p>Filters implicitly by the worker's assigned department ID.
     * <p>Response: 200 OK
     */
    @GetMapping
    @PreAuthorize("hasRole('WORKER') or hasRole('ADMIN')")
    public ResponseEntity<Page<ComplaintSummaryResponse>> getDepartmentQueue(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        log.info("Worker [{}] fetching department queue: status={}", authentication.getName(), status);
        int cappedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ResponseEntity.ok(complaintService.getDepartmentQueue(authentication.getName(), status, pageable));
    }

    /**
     * Update Complaint Status & Resolve via Worker route
     *
     * <p>Endpoint: PUT /api/worker/complaints/{id}/status
     * <p>Role/Auth: WORKER
     * <p>Request: Multipart Form-Data (status, afterImage, workerNotes)
     * <p>Response: 200 OK
     */
    @PutMapping(value = "/{id}/status", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('WORKER') or hasRole('ADMIN')")
    public ResponseEntity<ComplaintStatusUpdatedResponse> updateStatusViaWorkerPath(
            @PathVariable Long id,
            @ModelAttribute @Valid UpdateComplaintStatusForm form,
            Authentication authentication) {

        log.info("Worker [{}] updating complaint #{} via worker route", authentication.getName(), id);
        return ResponseEntity.ok(complaintService.updateStatus(id, form, authentication.getName()));
    }
}
