package com.example.civicsense.mapper;

import com.example.civicsense.dto.response.ComplaintDetailResponse;
import com.example.civicsense.dto.response.ComplaintSummaryResponse;
import com.example.civicsense.entity.Complaint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper for converting {@link Complaint} entities to response DTOs.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ComplaintMapper {

    /**
     * Maps a Complaint entity to the lightweight summary DTO used in paginated lists.
     */
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "duplicateCount", expression = "java(complaint.getDuplicates() != null ? complaint.getDuplicates().size() : 0)")
    ComplaintSummaryResponse toSummaryResponse(Complaint complaint);

    /**
     * Maps a Complaint entity to the full detail DTO used in the single-item endpoint.
     */
    @Mapping(target = "departmentName",    source = "department.name")
    @Mapping(target = "department.id",     source = "department.id")
    @Mapping(target = "department.name",   source = "department.name")
    @Mapping(target = "reporterName",      source = "reporter.name")
    @Mapping(target = "reporterEmail",     source = "reporter.email")
    @Mapping(target = "parentComplaintId", source = "parentComplaint.id")
    @Mapping(target = "duplicates",        source = "duplicates")
    ComplaintDetailResponse toDetailResponse(Complaint complaint);

    /** Maps a list of complaints to summary DTOs (used for the duplicates list). */
    List<ComplaintSummaryResponse> toSummaryResponseList(List<Complaint> complaints);
}
