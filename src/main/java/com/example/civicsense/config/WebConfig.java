package com.example.civicsense.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves locally uploaded files as static resources.
 * This makes {@code http://localhost:8080/uploads/**} serve files from the
 * configured local upload directory — useful during development without S3.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.local-upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadDir + "/");
    }
}
