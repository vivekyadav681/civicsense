package com.example.civicsense.repository;

import com.example.civicsense.entity.Complaint;
import com.example.civicsense.enums.ComplaintCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ComplaintRepositoryTest {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Test
    @DisplayName("findNearbyOpenComplaintsInCategory should execute Haversine SQL successfully without GROUP BY errors")
    void testFindNearbyOpenComplaintsInCategory() {
        // Gurgaon Cyber Hub coordinates: 28.4949, 77.0895 (seeded in V4 migration)
        List<Complaint> nearby = complaintRepository.findNearbyOpenComplaintsInCategory(
                ComplaintCategory.WASTE,
                28.4949,
                77.0895,
                100.0 // 100 meters
        );

        assertThat(nearby).isNotNull();
        // The query should execute cleanly without any PostgreSQL syntax or GROUP BY errors
    }
}
