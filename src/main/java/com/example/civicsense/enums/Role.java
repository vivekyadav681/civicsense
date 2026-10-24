package com.example.civicsense.enums;

/**
 * Defines the access levels in the CivicSense platform.
 * Used by Spring Security for role-based authorisation.
 */
public enum Role {
    /** A regular citizen who can file and track complaints. */
    CITIZEN,
    /** A government field-worker who can update complaint status and upload evidence. */
    WORKER,
    /** A system administrator with full access. */
    ADMIN
}
