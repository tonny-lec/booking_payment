package com.booking.shared.adapter.web.openapi;

import com.booking.shared.adapter.web.config.OpenApiValidationProperties;
import com.github.erosb.kappa.autoconfigure.EnableKappaRequestValidation;
import com.github.erosb.kappa.autoconfigure.KappaSpringConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;

/**
 * Enables OpenAPI-based request validation for Slice A endpoints.
 */
@Configuration
@EnableKappaRequestValidation
public class OpenApiValidationConfig {

    private final OpenApiValidationProperties properties;

    public OpenApiValidationConfig(OpenApiValidationProperties properties) {
        this.properties = properties;
    }

    @Bean
    public KappaSpringConfiguration kappaConfig() {
        var config = new KappaSpringConfiguration();
        var mapping = new LinkedHashMap<>(properties.getPathToSpec());
        config.setOpenapiDescriptions(mapping);
        if (!properties.getIgnoredPathPatterns().isEmpty()) {
            config.setIgnoredPathPatterns(
                    properties.getIgnoredPathPatterns().toArray(new String[0])
            );
        }
        return config;
    }
}
