package com.example.civicsense.repository;

import com.example.civicsense.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for {@link Department} entities.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * Looks up a department by its exact name.
     * Used by the routing engine to resolve department by category name.
     *
     * @param name department name (e.g. "Sanitation")
     */
    Optional<Department> findByName(String name);
}
