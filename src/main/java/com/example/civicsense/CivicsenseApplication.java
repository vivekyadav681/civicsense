package com.example.civicsense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * CivicSense — Civic Complaint Tracking Platform.
 *
 * <p>Entry point for the Spring Boot application.
 * Automatically loads key-value pairs from .env into system properties if present,
 * ensuring seamless execution in IDEs (IntelliJ, Eclipse, VS Code) without requiring
 * manual environment variable configuration.
 */
@SpringBootApplication
public class CivicsenseApplication {

    static {
        loadDotEnv();
    }

    public static void main(String[] args) {
        SpringApplication.run(CivicsenseApplication.class, args);
    }

    /**
     * Reads the local .env file (if it exists) and populates System properties
     * for any keys not already present in the environment or system properties.
     */
    public static void loadDotEnv() {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(envPath);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String val = line.substring(eq + 1).trim();
                    if ((val.startsWith("\"") && val.endsWith("\"")) ||
                        (val.startsWith("'") && val.endsWith("'"))) {
                        val = val.substring(1, val.length() - 1);
                    }
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, val);
                    }
                }
            }
        } catch (Exception ignored) {
            // Non-fatal; fallback to standard Spring environment resolution
        }
    }
}
