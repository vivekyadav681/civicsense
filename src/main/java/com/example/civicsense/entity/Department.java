package com.example.civicsense.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A civic government department responsible for resolving specific complaint categories.
 * Seeded via data.sql on startup; mapped to complaint categories by {@link com.example.civicsense.service.impl.ComplaintServiceImpl}.
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable department name, e.g. "Sanitation", "Public Works". */
    @Column(nullable = false, unique = true, length = 100)
    private String name;
}
