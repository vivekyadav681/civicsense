package com.example.civicsense.service;

import com.example.civicsense.dto.request.CreateComplaintRequest;
import com.example.civicsense.dto.request.UpdateStatusRequest;
import com.example.civicsense.dto.response.ComplaintDetailResponse;
import com.example.civicsense.dto.response.ComplaintSummaryResponse;
import com.example.civicsense.enums.ComplaintCategory;
import com.example.civicsense.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Defines the business operations for the complaint lifecycle.
 * Separating interface from implementation allows easy unit-testing via mocking.
 */
public interface ComplaintService {

    /**
     * Creates a new complaint, runs deduplication, routes to a department,
     * and uploads the before-image.
     *
     * @param request     validated complaint creation data
     * @param beforeImage the "before" photo evidence (required)
     * @param reporterEmail the email of the authenticated citizen making the report
     * @return the full detail response of the created complaint
     */
    ComplaintDetailResponse createComplaint(
            CreateComplaintRequest request,
            MultipartFile beforeImage,
            String reporterEmail);

    /**
     * Returns a paginated, filtered list of top-level (non-duplicate) complaints.
     *
     * @param status   optional filter by complaint status
     * @param category optional filter by complaint category
     * @param deptId   optional filter by department ID
     * @param pageable pagination and sorting parameters
     */
    Page<ComplaintSummaryResponse> getComplaints(
            ComplaintStatus status,
            ComplaintCategory category,
            Long deptId,
            Pageable pageable);

    /**
     * Returns the full detail of a complaint, including its list of duplicates.
     *
     * @param id the complaint ID
     * @throws com.example.civicsense.exception.ResourceNotFoundException if not found
     */
    ComplaintDetailResponse getComplaintById(Long id);

    /**
     * Updates the status of a complaint, enforcing the state machine rules.
     * Requires the after-image when transitioning to RESOLVED.
     *
     * @param id         the complaint ID
     * @param request    the desired new status
     * @param afterImage optional "after" photo evidence (required for RESOLVED)
     * @return the updated full detail response
     */
    ComplaintDetailResponse updateStatus(
            Long id,
            UpdateStatusRequest request,
            MultipartFile afterImage);
}
