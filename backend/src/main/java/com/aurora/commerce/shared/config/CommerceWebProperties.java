package com.aurora.commerce.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "commerce.web")
public record CommerceWebProperties(List<String> allowedOrigins) {

    public CommerceWebProperties {
        allowedOrigins = List.copyOf(allowedOrigins);
    }
}
