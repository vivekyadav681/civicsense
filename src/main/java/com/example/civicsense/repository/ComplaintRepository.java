package com.example.civicsense.repository;

import com.example.civicsense.entity.Complaint;
import com.example.civicsense.enums.ComplaintCategory;
import com.example.civicsense.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for {@link Complaint} entities.
 *
 * <p>The most important method here is {@link #findNearbyOpenComplaintsInCategory},
 * which powers the deduplication engine. It uses the <b>Haversine formula</b> embedded
 * in a native SQL query to compute the great-circle distance between two GPS coordinates
 * entirely inside the database — no PostGIS extension required.
 *
 * <h3>Haversine Formula (reference)</h3>
 * <pre>
 *   a = sin²(Δlat/2) + cos(lat1)·cos(lat2)·sin²(Δlng/2)
 *   c = 2·atan2(√a, √(1−a))
 *   d = R·c          where R = 6371000 m (Earth's mean radius)
 * </pre>
 */
@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // ─────────────────────────────────────────────────────────────────────────
    // DEDUPLICATION QUERY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Finds existing open complaints of the same category within a given radius.
     *
     * <p>This is a <b>native PostgreSQL query</b> using the Haversine formula.
     * The query:
     * <ol>
     *   <li>Restricts to complaints in the same category and NOT resolved/closed.</li>
     *   <li>Restricts to top-level (parent) complaints only (parent_complaint_id IS NULL)
     *       so duplicates don't spawn further chains.</li>
     *   <li>Orders by distance ASC so the closest existing complaint is linked as parent.</li>
     * </ol>
     *
     * @param category     the category of the new complaint
     * @param latitude     GPS latitude of the new complaint (degrees)
     * @param longitude    GPS longitude of the new complaint (degrees)
     * @param radiusMetres search radius in metres (typically 50)
     * @return list of candidate parent complaints, closest first
     */
    @Query(value = """
        SELECT c.*,
               (6371000 * acos(
                   LEAST(1.0, cos(radians(:lat)) * cos(radians(c.latitude))
                   * cos(radians(c.longitude) - radians(:lng))
                   + sin(radians(:lat)) * sin(radians(c.latitude)))
               )) AS distance_metres
        FROM complaints c
        WHERE c.category             = :#{#category.name()}
          AND c.status NOT IN ('RESOLVED', 'CLOSED')
          AND c.parent_complaint_id  IS NULL
        HAVING (6371000 * acos(
                   LEAST(1.0, cos(radians(:lat)) * cos(radians(c.latitude))
                   * cos(radians(c.longitude) - radians(:lng))
                   + sin(radians(:lat)) * sin(radians(c.latitude)))
               )) <= :radiusMetres
        ORDER BY distance_metres ASC
        """,
        nativeQuery = true)
    List<Complaint> findNearbyOpenComplaintsInCategory(
            @Param("category")     ComplaintCategory category,
            @Param("lat")          double latitude,
            @Param("lng")          double longitude,
            @Param("radiusMetres") double radiusMetres
    );

    // ─────────────────────────────────────────────────────────────────────────
    // LISTING / FILTERING QUERIES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Paginated list of complaints with optional filtering by status, category,
     * and department. All parameters are optional — pass null to skip a filter.
     *
     * <p>Only top-level (parent) complaints are returned; duplicates are accessible
     * through the detail endpoint ({@code GET /api/complaints/{id}}).
     */
    @Query("""
        SELECT c FROM Complaint c
        WHERE (:status   IS NULL OR c.status     = :status)
          AND (:category IS NULL OR c.category   = :category)
          AND (:deptId   IS NULL OR c.department.id = :deptId)
          AND c.parentComplaint IS NULL
        ORDER BY c.createdAt DESC
        """)
    Page<Complaint> findAllFiltered(
            @Param("status")   ComplaintStatus status,
            @Param("category") ComplaintCategory category,
            @Param("deptId")   Long deptId,
            Pageable pageable
    );

    /**
     * Fetches all duplicate (child) complaints linked to a given parent.
     * Used by the detail endpoint to show the full picture of a complaint.
     */
    List<Complaint> findByParentComplaintId(Long parentComplaintId);

    /**
     * Finds complaints within a given radius using the Haversine formula,
     * supporting optional category and status filters.
     */
    @Query(value = """
        SELECT c.*
        FROM complaints c
        WHERE (:category IS NULL OR c.category = :category)
          AND (
              :status IS NULL
              OR (:status = 'OPEN' AND c.status NOT IN ('RESOLVED', 'CLOSED'))
              OR (c.status = :status)
          )
          AND c.parent_complaint_id IS NULL
          AND (
              (6371000 * acos(
                  LEAST(1.0, cos(radians(:lat)) * cos(radians(c.latitude))
                  * cos(radians(c.longitude) - radians(:lng))
                  + sin(radians(:lat)) * sin(radians(c.latitude)))
              )) <= :radiusMetres
          )
        ORDER BY c.created_at DESC
        """,
        countQuery = """
        SELECT count(*)
        FROM complaints c
        WHERE (:category IS NULL OR c.category = :category)
          AND (
              :status IS NULL
              OR (:status = 'OPEN' AND c.status NOT IN ('RESOLVED', 'CLOSED'))
              OR (c.status = :status)
          )
          AND c.parent_complaint_id IS NULL
          AND (
              (6371000 * acos(
                  LEAST(1.0, cos(radians(:lat)) * cos(radians(c.latitude))
                  * cos(radians(c.longitude) - radians(:lng))
                  + sin(radians(:lat)) * sin(radians(c.latitude)))
              )) <= :radiusMetres
          )
        """,
        nativeQuery = true)
    Page<Complaint> findNearbyComplaintsFeed(
            @Param("category")     String category,
            @Param("status")       String status,
            @Param("lat")          double lat,
            @Param("lng")          double lng,
            @Param("radiusMetres") double radiusMetres,
            Pageable pageable
    );

    // ─────────────────────────────────────────────────────────────────────────
    // MY COMPLAINTS (Citizen)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of all complaints filed by a specific reporter,
     * ordered newest-first.
     */
    @Query("""
        SELECT c FROM Complaint c
        WHERE c.reporter.email = :email
        ORDER BY c.createdAt DESC
        """)
    Page<Complaint> findByReporterEmail(
            @Param("email") String email,
            Pageable pageable
    );

    // ─────────────────────────────────────────────────────────────────────────
    // DEPARTMENT QUEUE (Worker)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of top-level complaints assigned to a specific department,
     * with an optional status filter.
     */
    @Query("""
        SELECT c FROM Complaint c
        WHERE c.department.id = :deptId
          AND (:status IS NULL OR c.status = :status)
          AND c.parentComplaint IS NULL
        ORDER BY c.createdAt ASC
        """)
    Page<Complaint> findByDepartmentIdAndOptionalStatus(
            @Param("deptId")  Long deptId,
            @Param("status")  ComplaintStatus status,
            Pageable pageable
    );
}
