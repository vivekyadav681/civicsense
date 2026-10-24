package com.example.civicsense.entity;

import com.example.civicsense.enums.ComplaintCategory;
import com.example.civicsense.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Core entity representing a civic complaint filed by a citizen.
 *
 * <p>Key design decisions:
 * <ul>
 *   <li><b>Deduplication</b>: A complaint found to be within 50 m of an existing open
 *       complaint of the same category becomes a "child" of that parent via
 *       {@code parentComplaint}. Workers see only parent complaints, keeping their
 *       queue clean while all duplicate reports are preserved for audit.</li>
 *   <li><b>Geolocation</b>: {@code latitude}/{@code longitude} are stored as plain
 *       {@code Double} columns. Spatial proximity is computed via the Haversine formula
 *       inside a native JPQL query in {@link com.example.civicsense.repository.ComplaintRepository}.
 *       This avoids adding the PostGIS extension as a hard dependency.</li>
 *   <li><b>Image evidence</b>: {@code beforeImageUrl} is set at submission time.
 *       {@code afterImageUrl} is required before transitioning to {@code RESOLVED}.</li>
 * </ul>
 */
@Entity
@Table(name = "complaints", indexes = {
    @Index(name = "idx_complaints_status",   columnList = "status"),
    @Index(name = "idx_complaints_category", columnList = "category"),
    @Index(name = "idx_complaints_reporter", columnList = "reporter_id"),
    @Index(name = "idx_complaints_coords",   columnList = "latitude,longitude")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Issue category, used for deduplication matching and department routing. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplaintCategory category;

    /** GPS latitude of the issue location. */
    @Column(nullable = false)
    private Double latitude;

    /** GPS longitude of the issue location. */
    @Column(nullable = false)
    private Double longitude;

    /** Current lifecycle state of the complaint. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.REPORTED;

    /** S3/filesystem URL of the "before" photo uploaded at report time. */
    @Column(length = 2048)
    private String beforeImageUrl;

    /**
     * S3/filesystem URL of the "after" photo uploaded by a worker as resolution evidence.
     * Mandatory for transition to {@link ComplaintStatus#RESOLVED}.
     */
    @Column(length = 2048)
    private String afterImageUrl;

    // ── Relationships ────────────────────────────────────────────────────────

    /** The department responsible for resolving this complaint. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", foreignKey = @ForeignKey(name = "fk_complaint_department"))
    private Department department;

    /** The citizen who filed this complaint. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_complaint_reporter"))
    private User reporter;

    /**
     * If this complaint is a duplicate detected within 50 m of an existing open
     * complaint of the same category, this field points to that "parent" complaint.
     * Null for original (parent) complaints.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_complaint_id",
                foreignKey = @ForeignKey(name = "fk_complaint_parent"))
    private Complaint parentComplaint;

    /**
     * All complaints that were linked to this one as duplicates.
     * Populated lazily; useful for the detail endpoint that must show duplicate count.
     */
    @OneToMany(mappedBy = "parentComplaint", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<Complaint> duplicates = new ArrayList<>();

    // ── Audit timestamps ─────────────────────────────────────────────────────

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
