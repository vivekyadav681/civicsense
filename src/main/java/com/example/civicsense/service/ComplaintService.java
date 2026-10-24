package com.example.civicsense.service;

import com.example.civicsense.dto.request.CreateComplaintForm;
import com.example.civicsense.dto.request.UpdateComplaintStatusForm;
import com.example.civicsense.dto.response.ComplaintCreatedResponse;
import com.example.civicsense.dto.response.ComplaintDetailResponse;
import com.example.civicsense.dto.response.ComplaintStatusUpdatedResponse;
import com.example.civicsense.dto.response.ComplaintSummaryResponse;
import com.example.civicsense.enums.ComplaintCategory;
import com.example.civicsense.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service contract for the civic complaint tracking lifecycle.
 */
public interface ComplaintService {

    /**
     * Files a new complaint using multipart data (including beforeImage uploaded to Cloudinary),
     * runs deduplication detection, and auto-routes to the designated department.
     *
     * @param form          multipart form containing complaint details and beforeImage file
     * @param reporterEmail email of authenticated citizen
     * @return creation confirmation with duplicate grouping message
     */
    ComplaintCreatedResponse createComplaint(CreateComplaintForm form, String reporterEmail);

    /**
     * Returns a paginated public feed of top-level complaints with optional category,
     * status, and GPS radius search.
     *
     * @param category optional category filter
     * @param status   optional status filter (e.g. "OPEN" or specific status)
     * @param lat      optional GPS latitude
     * @param lng      optional GPS longitude
     * @param radius   optional search radius in metres
     * @param pageable pagination and sorting parameters
     * @return paginated list of complaints
     */
    Page<ComplaintSummaryResponse> getComplaints(
            ComplaintCategory category,
            String status,
            Double lat,
            Double lng,
            Double radius,
            Pageable pageable);

    /**
     * Returns full details of a complaint along with its duplicates.
     *
     * @param id complaint ID
     * @return full details response
     */
    ComplaintDetailResponse getComplaintById(Long id);

    /**
     * Returns complaints filed by the authenticated citizen.
     *
     * @param reporterEmail citizen email extracted from JWT
     * @param pageable      pagination parameters
     * @return paginated citizen complaints
     */
    Page<ComplaintSummaryResponse> getMyComplaints(String reporterEmail, Pageable pageable);

    /**
     * Returns the department triage queue for the authenticated worker,
     * implicitly filtered by the worker's assigned department.
     *
     * @param workerEmail worker email extracted from JWT
     * @param status      optional status filter (e.g. ASSIGNED, IN_PROGRESS)
     * @param pageable    pagination parameters
     * @return paginated complaints in worker's queue
     */
    Page<ComplaintSummaryResponse> getDepartmentQueue(
            String workerEmail,
            ComplaintStatus status,
            Pageable pageable);

    /**
     * Updates complaint lifecycle status and uploads resolution evidence photo (afterImage)
     * to Cloudinary when transitioning to RESOLVED.
     *
     * @param id          complaint ID
     * @param form        multipart form containing status, afterImage, and worker notes
     * @param workerEmail email of worker performing the update
     * @return status update confirmation
     */
    ComplaintStatusUpdatedResponse updateStatus(
            Long id,
            UpdateComplaintStatusForm form,
            String workerEmail);
}
