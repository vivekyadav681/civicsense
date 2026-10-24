package com.example.civicsense.mapper;

import com.example.civicsense.dto.response.ComplaintDetailResponse;
import com.example.civicsense.dto.response.ComplaintSummaryResponse;
import com.example.civicsense.entity.Complaint;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Spring Component for converting {@link Complaint} entities to response DTOs.
 * Implemented directly to guarantee bean registration across all IDEs and compilers
 * without relying on compile-time annotation processing.
 */
@Component
public class ComplaintMapper {

    /**
     * Maps a Complaint entity to the lightweight summary DTO used in feeds and lists.
     */
    public ComplaintSummaryResponse toSummaryResponse(Complaint complaint) {
        if (complaint == null) {
            return null;
        }

        int duplicateCount = (complaint.getDuplicates() != null) ? complaint.getDuplicates().size() : 0;
        String departmentName = (complaint.getDepartment() != null) ? complaint.getDepartment().getName() : null;

        return ComplaintSummaryResponse.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .category(complaint.getCategory())
                .status(complaint.getStatus())
                .latitude(complaint.getLatitude())
                .longitude(complaint.getLongitude())
                .beforeImageUrl(complaint.getBeforeImageUrl())
                .departmentName(departmentName)
                .duplicateCount(duplicateCount)
                .createdAt(complaint.getCreatedAt())
                .build();
    }

    /**
     * Maps a Complaint entity to the full detail DTO with duplicates and department object.
     */
    public ComplaintDetailResponse toDetailResponse(Complaint complaint) {
        if (complaint == null) {
            return null;
        }

        String deptName = (complaint.getDepartment() != null) ? complaint.getDepartment().getName() : null;
        Long deptId = (complaint.getDepartment() != null) ? complaint.getDepartment().getId() : null;
        ComplaintDetailResponse.DepartmentInfo deptInfo = (complaint.getDepartment() != null)
                ? new ComplaintDetailResponse.DepartmentInfo(deptId, deptName)
                : null;

        String repName = (complaint.getReporter() != null) ? complaint.getReporter().getName() : null;
        String repEmail = (complaint.getReporter() != null) ? complaint.getReporter().getEmail() : null;
        Long parentId = (complaint.getParentComplaint() != null) ? complaint.getParentComplaint().getId() : null;

        List<ComplaintSummaryResponse> duplicates = (complaint.getDuplicates() != null)
                ? toSummaryResponseList(complaint.getDuplicates())
                : Collections.emptyList();

        return ComplaintDetailResponse.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .status(complaint.getStatus())
                .latitude(complaint.getLatitude())
                .longitude(complaint.getLongitude())
                .beforeImageUrl(complaint.getBeforeImageUrl())
                .afterImageUrl(complaint.getAfterImageUrl())
                .workerNotes(complaint.getWorkerNotes())
                .department(deptInfo)
                .departmentName(deptName)
                .reporterName(repName)
                .reporterEmail(repEmail)
                .parentComplaintId(parentId)
                .duplicates(duplicates)
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .build();
    }

    /**
     * Maps a list of complaints to summary DTOs.
     */
    public List<ComplaintSummaryResponse> toSummaryResponseList(List<Complaint> complaints) {
        if (complaints == null) {
            return Collections.emptyList();
        }
        return complaints.stream().map(this::toSummaryResponse).toList();
    }
}
