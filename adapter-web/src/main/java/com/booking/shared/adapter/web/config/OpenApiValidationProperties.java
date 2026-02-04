package com.booking.shared.adapter.web.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for OpenAPI request validation mapping.
 */
@Component
@ConfigurationProperties(prefix = "app.openapi.validation")
@Validated
public class OpenApiValidationProperties {

    @NotEmpty
    private Map<String, String> pathToSpec = new LinkedHashMap<>();

    private List<String> ignoredPathPatterns = new ArrayList<>();

    public Map<String, String> getPathToSpec() {
        return pathToSpec;
    }

    public void setPathToSpec(Map<String, String> pathToSpec) {
        this.pathToSpec = pathToSpec;
    }

    public List<String> getIgnoredPathPatterns() {
        return ignoredPathPatterns;
    }

    public void setIgnoredPathPatterns(List<String> ignoredPathPatterns) {
        this.ignoredPathPatterns = ignoredPathPatterns;
    }
}
