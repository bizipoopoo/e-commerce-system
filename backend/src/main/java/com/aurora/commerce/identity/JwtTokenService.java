package com.aurora.commerce.identity;

import com.aurora.commerce.shared.config.CommerceSecurityProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final CommerceSecurityProperties properties;
    private final Clock clock = Clock.systemUTC();

    JwtTokenService(JwtEncoder jwtEncoder, CommerceSecurityProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    Token issue(UserAccount user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("aurora-commerce")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.email())
                .claim("userId", user.id())
                .claim("roles", List.of(user.role().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Token(value, properties.accessTokenTtl().toSeconds());
    }

    record Token(String value, long expiresInSeconds) {
    }
}
