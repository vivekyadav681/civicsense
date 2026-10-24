package com.example.civicsense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CivicSense — Civic Complaint Tracking Platform.
 *
 * <p>Entry point for the Spring Boot application.
 * For local development, ensure a PostgreSQL instance is running (see application.yaml)
 * or configure an in-memory H2 datasource for quick testing.
 */
@SpringBootApplication
public class CivicsenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(CivicsenseApplication.class, args);
    }
}
