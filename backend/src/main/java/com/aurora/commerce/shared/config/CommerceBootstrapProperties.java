package com.aurora.commerce.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "commerce.bootstrap")
public record CommerceBootstrapProperties(boolean enabled, String adminEmail, String adminPassword) {
}
