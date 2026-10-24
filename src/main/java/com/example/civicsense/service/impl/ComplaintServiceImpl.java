package com.example.civicsense.service.impl;

import com.example.civicsense.dto.request.CreateComplaintRequest;
import com.example.civicsense.dto.request.UpdateStatusRequest;
import com.example.civicsense.dto.response.ComplaintDetailResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core business logic implementation for the civic complaint lifecycle.
 *
 * <h2>Key responsibilities</h2>
 * <ol>
 *   <li><b>Deduplication Engine</b>: Before persisting a new complaint, scans the
 *       database for existing open complaints of the same category within a configurable
 *       radius (default 50 m) using the Haversine formula native query. If a match is
 *       found, the new complaint is saved as a "child" of the nearest existing one.</li>
 *   <li><b>Automated Department Routing</b>: Maps each ComplaintCategory to a specific
 *       Department so complaints are immediately assigned on creation.</li>
 *   <li><b>State Machine</b>: Enforces the valid lifecycle transitions and validates
 *       that resolution evidence (afterImage) is present before marking RESOLVED.</li>
 * </ol>
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

    /**
     * Valid forward transitions.
     * REPORTED -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED
     * Transitions not in this map are forbidden.
     */
    private static final Map<ComplaintStatus, Set<ComplaintStatus>> VALID_TRANSITIONS =
            new EnumMap<>(ComplaintStatus.class);

    static {
        VALID_TRANSITIONS.put(ComplaintStatus.REPORTED,    Set.of(ComplaintStatus.ASSIGNED));
        VALID_TRANSITIONS.put(ComplaintStatus.ASSIGNED,    Set.of(ComplaintStatus.IN_PROGRESS));
        VALID_TRANSITIONS.put(ComplaintStatus.IN_PROGRESS, Set.of(ComplaintStatus.RESOLVED));
        VALID_TRANSITIONS.put(ComplaintStatus.RESOLVED,    Set.of(ComplaintStatus.CLOSED));
        VALID_TRANSITIONS.put(ComplaintStatus.CLOSED,      Set.of());
    }

    // =========================================================================
    // CREATE COMPLAINT
    // =========================================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('CITIZEN') or hasRole('ADMIN')")
    public ComplaintDetailResponse createComplaint(
            CreateComplaintRequest request,
            MultipartFile beforeImage,
            String reporterEmail) {

        log.info("Creating complaint: category={}, lat={}, lng={}",
                request.getCategory(), request.getLatitude(), request.getLongitude());

        // 1. Resolve the reporter
        User reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", -1L));

        // 2. Resolve before-image: prefer direct S3 URL from client, fallback to multipart upload
        String beforeImageUrl = null;
        if (StringUtils.hasText(request.getBeforeImageUrl())) {
            beforeImageUrl = request.getBeforeImageUrl();
            log.info("Using direct S3 beforeImageUrl from client: {}", beforeImageUrl);
        } else if (beforeImage != null && !beforeImage.isEmpty()) {
            beforeImageUrl = storageService.store(beforeImage, "before");
        }

        // 3. DEDUPLICATION ENGINE
        Complaint parentComplaint = findNearestDuplicate(
                request.getCategory(), request.getLatitude(), request.getLongitude());

        if (parentComplaint != null) {
            log.info("Duplicate detected — linking new complaint to parent ID={}", parentComplaint.getId());
        }

        // 4. AUTOMATED DEPARTMENT ROUTING
        Department department = resolveDepartment(request.getCategory());

        // 5. Persist
        Complaint complaint = Complaint.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(ComplaintStatus.REPORTED)
                .beforeImageUrl(beforeImageUrl)
                .reporter(reporter)
                .department(department)
                .parentComplaint(parentComplaint)
                .build();

        Complaint saved = complaintRepository.save(complaint);

        // Auto-assign top-level complaints that have a department
        if (parentComplaint == null && department != null) {
            saved.setStatus(ComplaintStatus.ASSIGNED);
            saved = complaintRepository.save(saved);
        }

        log.info("Complaint saved: id={}, isDuplicate={}", saved.getId(), parentComplaint != null);
        return complaintMapper.toDetailResponse(saved);
    }

    // =========================================================================
    // GET COMPLAINTS (Paginated + Filtered)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ComplaintSummaryResponse> getComplaints(
            ComplaintStatus status, ComplaintCategory category,
            Long deptId, Pageable pageable) {

        return complaintRepository
                .findAllFiltered(status, category, deptId, pageable)
                .map(complaintMapper::toSummaryResponse);
    }

    // =========================================================================
    // GET COMPLAINT BY ID
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
    // UPDATE STATUS (State Machine + Evidence Validation)
    // =========================================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('WORKER') or hasRole('ADMIN')")
    public ComplaintDetailResponse updateStatus(
            Long id, UpdateStatusRequest request, MultipartFile afterImage) {

        Complaint complaint    = findComplaintOrThrow(id);
        ComplaintStatus current = complaint.getStatus();
        ComplaintStatus target  = request.getStatus();

        log.info("Status update: id={}, {} -> {}", id, current, target);

        // Enforce state machine
        validateTransition(current, target);

        // Evidence gate: RESOLVED requires an after-image
        if (target == ComplaintStatus.RESOLVED) {
            if (StringUtils.hasText(request.getAfterImageUrl())) {
                complaint.setAfterImageUrl(request.getAfterImageUrl());
                log.info("Using direct S3 afterImageUrl from client: {}", request.getAfterImageUrl());
            } else if (afterImage != null && !afterImage.isEmpty()) {
                complaint.setAfterImageUrl(storageService.store(afterImage, "after"));
                log.info("After-image stored for complaint id={}", id);
            } else if (!StringUtils.hasText(complaint.getAfterImageUrl())) {
                throw new IllegalStateTransitionException(
                    "Cannot mark complaint RESOLVED without an after-evidence photo.");
            }
        }

        complaint.setStatus(target);
        Complaint updated = complaintRepository.save(complaint);
        log.info("Status updated: id={}, newStatus={}", id, target);
        return complaintMapper.toDetailResponse(updated);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Queries for the nearest open parent complaint within the deduplication radius.
     * Returns null if no duplicate is found — the new complaint becomes a parent.
     */
    private Complaint findNearestDuplicate(ComplaintCategory category, double lat, double lng) {
        List<Complaint> nearby = complaintRepository.findNearbyOpenComplaintsInCategory(
                category, lat, lng, deduplicationRadiusMetres);
        if (nearby.isEmpty()) {
            return null;
        }
        Complaint nearest = nearby.get(0);
        log.debug("Nearest duplicate: id={}, within {}m", nearest.getId(), deduplicationRadiusMetres);
        return nearest;
    }

    /**
     * Maps a ComplaintCategory to its owning Department.
     * Department names are configurable via application.yaml app.departments.* keys.
     */
    private Department resolveDepartment(ComplaintCategory category) {
        String deptName = switch (category) {
            case WASTE        -> sanitationDeptName;
            case ROADS        -> publicWorksDeptName;
            case STREETLIGHTS -> electricalDeptName;
            case OTHER        -> generalAdminDeptName;
        };
        return departmentRepository.findByName(deptName)
                .orElseGet(() -> {
                    log.warn("Department '{}' not found. Complaint will be unassigned.", deptName);
                    return null;
                });
    }

    /** Validates that the requested status transition is allowed by the state machine. */
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
