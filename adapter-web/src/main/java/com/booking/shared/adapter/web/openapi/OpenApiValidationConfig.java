package com.booking.shared.adapter.web.openapi;

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

    @Bean
    public KappaSpringConfiguration kappaConfig() {
        var config = new KappaSpringConfiguration();
        var mapping = new LinkedHashMap<String, String>();
        mapping.put("/api/v1/auth/**", "/static/openapi/iam.yaml");
        mapping.put("/api/v1/bookings/**", "/static/openapi/booking.yaml");
        mapping.put("/api/v1/payments/**", "/static/openapi/payment.yaml");
        config.setOpenapiDescriptions(mapping);
        config.setIgnoredPathPatterns(
                "/actuator/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/openapi/**",
                "/error"
        );
        return config;
    }
}
