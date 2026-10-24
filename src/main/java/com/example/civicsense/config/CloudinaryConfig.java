package com.example.civicsense.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configures the Cloudinary SDK bean using credentials bound from
 * {@code application.yaml} (app.storage.cloudinary.*).
 *
 * <p>The resulting {@link Cloudinary} bean is injected into
 * {@link com.example.civicsense.storage.CloudinaryStorageService}.
 */
@Configuration
public class CloudinaryConfig {

    @Value("${app.storage.cloudinary.cloud-name}")
    private String cloudName;

    @Value("${app.storage.cloudinary.api-key}")
    private String apiKey;

    @Value("${app.storage.cloudinary.api-secret}")
    private String apiSecret;

    /**
     * Produces a fully-configured {@link Cloudinary} instance.
     * Credentials are supplied via environment variables; no plaintext secrets
     * should ever be committed to source control.
     */
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(Map.of(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true          // always use HTTPS URLs
        ));
    }
}
