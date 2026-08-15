package com.aurora.commerce.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

import static java.nio.charset.StandardCharsets.UTF_8;

@ConfigurationProperties(prefix = "commerce.security")
public record CommerceSecurityProperties(String jwtSecret, Duration accessTokenTtl) {

    public CommerceSecurityProperties {
        if (jwtSecret == null || jwtSecret.getBytes(UTF_8).length < 32) {
            throw new IllegalArgumentException("commerce.security.jwt-secret must contain at least 32 bytes");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalArgumentException("commerce.security.access-token-ttl must be positive");
        }
    }
}
