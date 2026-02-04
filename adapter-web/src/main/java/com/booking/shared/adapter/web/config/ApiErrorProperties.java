package com.booking.shared.adapter.web.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for RFC 7807 error responses.
 */
@Component
@ConfigurationProperties(prefix = "app.api-errors")
@Validated
public class ApiErrorProperties {

    @NotBlank
    private String typeBaseUri = "https://api.booking-payment.com/errors/";

    private boolean includeTraceId = true;

    private boolean includeTimestamp = true;

    public String getTypeBaseUri() {
        return typeBaseUri;
    }

    public void setTypeBaseUri(String typeBaseUri) {
        this.typeBaseUri = typeBaseUri;
    }

    public boolean isIncludeTraceId() {
        return includeTraceId;
    }

    public void setIncludeTraceId(boolean includeTraceId) {
        this.includeTraceId = includeTraceId;
    }

    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }

    public void setIncludeTimestamp(boolean includeTimestamp) {
        this.includeTimestamp = includeTimestamp;
    }
}
