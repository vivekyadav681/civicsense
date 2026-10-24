package com.example.civicsense.service.impl;

import com.example.civicsense.dto.request.CreateComplaintForm;
import com.example.civicsense.dto.request.UpdateComplaintStatusForm;
import com.example.civicsense.dto.response.ComplaintCreatedResponse;
import com.example.civicsense.dto.response.ComplaintDetailResponse;
import com.example.civicsense.dto.response.ComplaintStatusUpdatedResponse;
import com.example.civicsense.dto.response.ComplaintSummaryResponse;
import com.example.civicsense.entity.Complaint;
import com.example.civicsense.entity.Department;
import com.example.civicsense.entity.User;
import com.example.civicsense.enums.ComplaintCategory;
import com.example.civicsense.enums.ComplaintStatus;
import com.example.civicsense.exception.IllegalStateTransitionException;
import com.example.civicsense.exception.ResourceNotFoundException;
import com.example.civicsense.mapper.ComplaintMapper;
import com.example.civicsense.repository.ComplaintRepository;
import com.example.civicsense.repository.DepartmentRepository;
import com.example.civicsense.repository.UserRepository;
import com.example.civicsense.service.ComplaintService;
import com.example.civicsense.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core business logic implementation for the civic complaint lifecycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository  complaintRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository       userRepository;
    private final StorageService       storageService;
    private final ComplaintMapper      complaintMapper;

    @Value("${app.deduplication.radius-metres:50}")
    private double deduplicationRadiusMetres;

    @Value("${app.departments.waste:Sanitation}")
    private String sanitationDeptName;

    @Value("${app.departments.roads:Public Works}")
    private String publicWorksDeptName;

    @Value("${app.departments.streetlights:Electrical}")
    private String electricalDeptName;

    @Value("${app.departments.other:General Administration}")
    private String generalAdminDeptName;

    // =========================================================================
    // STATE MACHINE DEFINITION
    // =========================================================================

    private static final Map<ComplaintStatus, Set<ComplaintStatus>> VALID_TRANSITIONS =
            new EnumMap<>(ComplaintStatus.class);

    static {
        VALID_TRANSITIONS.put(ComplaintStatus.REPORTED,    Set.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS));
        VALID_TRANSITIONS.put(ComplaintStatus.ASSIGNED,    Set.of(ComplaintStatus.IN_PROGRESS));
        VALID_TRANSITIONS.put(ComplaintStatus.IN_PROGRESS, Set.of(ComplaintStatus.RESOLVED));
        VALID_TRANSITIONS.put(ComplaintStatus.RESOLVED,    Set.of(ComplaintStatus.CLOSED));
        VALID_TRANSITIONS.put(ComplaintStatus.CLOSED,      Set.of());
    }

    // =========================================================================
    // 1. CREATE COMPLAINT (Citizen)
    // =========================================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('CITIZEN') or hasRole('ADMIN')")
    public ComplaintCreatedResponse createComplaint(CreateComplaintForm form, String reporterEmail) {
        log.info("Submitting complaint: category={}, lat={}, lng={}, reporter={}",
                form.getCategory(), form.getLatitude(), form.getLongitude(), reporterEmail);

        // 1. Resolve the citizen reporter
        User reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", -1L));

        // 2. Upload photo evidence to Cloudinary
        String beforeImageUrl = null;
        if (form.getBeforeImage() != null && !form.getBeforeImage().isEmpty()) {
            beforeImageUrl = storageService.store(form.getBeforeImage(), "before");
            log.info("Before-image uploaded to Cloudinary: {}", beforeImageUrl);
        }

        // 3. Deduplication Engine: scan for duplicate in 50m radius
        Complaint parentComplaint = findNearestDuplicate(
                form.getCategory(), form.getLatitude(), form.getLongitude());

        // 4. Automated Department Routing
        Department department = resolveDepartment(form.getCategory());

        // 5. Persist complaint
        Complaint complaint = Complaint.builder()
                .title(form.getTitle())
                .description(form.getDescription())
                .category(form.getCategory())
                .latitude(form.getLatitude())
                .longitude(form.getLongitude())
                .status(ComplaintStatus.REPORTED)
                .beforeImageUrl(beforeImageUrl)
                .reporter(reporter)
                .department(department)
                .parentComplaint(parentComplaint)
                .build();

        Complaint saved = complaintRepository.save(complaint);

        // Auto-assign top-level complaints if department resolved
        if (parentComplaint == null && department != null) {
            saved.setStatus(ComplaintStatus.ASSIGNED);
            saved = complaintRepository.save(saved);
        }

        String message = parentComplaint != null
                ? "Complaint recorded. Grouped with existing issue #" + parentComplaint.getId()
                : "Complaint recorded.";

        return ComplaintCreatedResponse.builder()
                .id(saved.getId())
                .status(saved.getStatus())
                .departmentId(saved.getDepartment() != null ? saved.getDepartment().getId() : null)
                .parentComplaintId(parentComplaint != null ? parentComplaint.getId() : null)
                .message(message)
                .build();
    }

    // =========================================================================
    // 2. GET COMPLAINTS (Feed / Map view)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ComplaintSummaryResponse> getComplaints(
            ComplaintCategory category,
            String status,
            Double lat,
            Double lng,
            Double radius,
            Pageable pageable) {

        // If GPS coordinates and radius are supplied, perform spatial Haversine search
        if (lat != null && lng != null && radius != null && radius > 0) {
            String catName = category != null ? category.name() : null;
            return complaintRepository.findNearbyComplaintsFeed(catName, status, lat, lng, radius, pageable)
                    .map(complaintMapper::toSummaryResponse);
        }

        // Standard filter query
        ComplaintStatus statusEnum = null;
        if (StringUtils.hasText(status) && !status.equalsIgnoreCase("OPEN")) {
            try {
                statusEnum = ComplaintStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fall through to null filter
            }
        }

        return complaintRepository.findAllFiltered(statusEnum, category, null, pageable)
                .map(complaintMapper::toSummaryResponse);
    }

    // =========================================================================
    // 3. GET COMPLAINT BY ID
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ComplaintDetailResponse getComplaintById(Long id) {
        Complaint complaint = findComplaintOrThrow(id);
        List<Complaint> duplicates = complaintRepository.findByParentComplaintId(id);
        complaint.setDuplicates(duplicates);
        return complaintMapper.toDetailResponse(complaint);
    }

    // =========================================================================
    // 4. GET MY SUBMITTED COMPLAINTS (Citizen)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('CITIZEN') or hasRole('ADMIN')")
    public Page<ComplaintSummaryResponse> getMyComplaints(String reporterEmail, Pageable pageable) {
        return complaintRepository.findByReporterEmail(reporterEmail, pageable)
                .map(complaintMapper::toSummaryResponse);
    }

    // =========================================================================
    // 5. GET DEPARTMENT QUEUE (Worker)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('WORKER') or hasRole('ADMIN')")
    public Page<ComplaintSummaryResponse> getDepartmentQueue(
            String workerEmail, ComplaintStatus status, Pageable pageable) {

        User worker = userRepository.findByEmail(workerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Worker", -1L));

        if (worker.getDepartment() == null) {
            throw new ResourceNotFoundException("Worker is not assigned to any department.");
        }

        return complaintRepository.findByDepartmentIdAndOptionalStatus(
                worker.getDepartment().getId(), status, pageable)
                .map(complaintMapper::toSummaryResponse);
    }

    // =========================================================================
    // 6. UPDATE STATUS & RESOLVE (Worker)
    // =========================================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('WORKER') or hasRole('ADMIN')")
    public ComplaintStatusUpdatedResponse updateStatus(
            Long id, UpdateComplaintStatusForm form, String workerEmail) {

        Complaint complaint = findComplaintOrThrow(id);
        ComplaintStatus current = complaint.getStatus();
        ComplaintStatus target = form.getStatus();

        log.info("Worker [{}] updating complaint id={}: {} -> {}", workerEmail, id, current, target);

        // Validate state machine transition
        validateTransition(current, target);

        // Evidence gate: RESOLVED requires afterImage photo evidence
        if (target == ComplaintStatus.RESOLVED) {
            if (form.getAfterImage() != null && !form.getAfterImage().isEmpty()) {
                String afterUrl = storageService.store(form.getAfterImage(), "after");
                complaint.setAfterImageUrl(afterUrl);
                log.info("Resolution after-image uploaded to Cloudinary: {}", afterUrl);
            } else if (!StringUtils.hasText(complaint.getAfterImageUrl())) {
                throw new IllegalStateTransitionException(
                        "Cannot mark complaint RESOLVED without an after-evidence photo.");
            }
        } else if (form.getAfterImage() != null && !form.getAfterImage().isEmpty()) {
            String afterUrl = storageService.store(form.getAfterImage(), "after");
            complaint.setAfterImageUrl(afterUrl);
        }

        if (StringUtils.hasText(form.getWorkerNotes())) {
            complaint.setWorkerNotes(form.getWorkerNotes());
        }

        complaint.setStatus(target);
        Complaint updated = complaintRepository.save(complaint);

        return ComplaintStatusUpdatedResponse.builder()
                .id(updated.getId())
                .status(updated.getStatus())
                .afterImageUrl(updated.getAfterImageUrl())
                .message("Status updated successfully")
                .build();
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Complaint findNearestDuplicate(ComplaintCategory category, double lat, double lng) {
        List<Complaint> nearby = complaintRepository.findNearbyOpenComplaintsInCategory(
                category, lat, lng, deduplicationRadiusMetres);
        if (nearby.isEmpty()) {
            return null;
        }
        Complaint nearest = nearby.get(0);
        log.info("Duplicate detected within {}m of existing complaint #{}",
                deduplicationRadiusMetres, nearest.getId());
        return nearest;
    }

    private Department resolveDepartment(ComplaintCategory category) {
        String deptName = switch (category) {
            case WASTE        -> sanitationDeptName;
            case ROADS        -> publicWorksDeptName;
            case STREETLIGHTS -> electricalDeptName;
            case OTHER        -> generalAdminDeptName;
        };
        return departmentRepository.findByName(deptName)
                .orElseGet(() -> {
                    log.warn("Department '{}' not found in database.", deptName);
                    return null;
                });
    }

    private void validateTransition(ComplaintStatus from, ComplaintStatus to) {
        Set<ComplaintStatus> allowed = VALID_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalStateTransitionException(from, to);
        }
    }

    private Complaint findComplaintOrThrow(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", id));
    }
}
