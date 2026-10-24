package com.example.civicsense.enums;

/**
 * Represents the lifecycle state of a civic complaint.
 *
 * Allowed transitions (state machine):
 *   REPORTED  → ASSIGNED
 *   ASSIGNED  → IN_PROGRESS
 *   IN_PROGRESS → RESOLVED  (requires afterImageUrl evidence)
 *   RESOLVED  → CLOSED
 */
public enum ComplaintStatus {
    REPORTED,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
