package com.aurora.commerce.shared.config;

import com.aurora.commerce.shared.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;

@Configuration
public class SecurityConfiguration {

    private final CommerceWebProperties webProperties;
    private final ObjectMapper objectMapper;

    public SecurityConfiguration(CommerceWebProperties webProperties, ObjectMapper objectMapper) {
        this.webProperties = webProperties;
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(this::handleUnauthorized)
                        .accessDeniedHandler(this::handleForbidden))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/system/overview",
                                "/api/v1/home",
                                "/api/v1/recommendations",
                                "/api/v1/content/feed",
                                "/api/v1/content/articles/**",
                                "/api/v1/categories",
                                "/api/v1/products",
                                "/api/v1/products/**",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(this::handleUnauthorized)
                        .accessDeniedHandler(this::handleForbidden)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(CommerceSecurityProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey(properties)));
    }

    @Bean
    JwtDecoder jwtDecoder(CommerceSecurityProperties properties) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    private SecretKey jwtSecretKey(CommerceSecurityProperties properties) {
        return new SecretKeySpec(properties.jwtSecret().getBytes(UTF_8), "HmacSHA256");
    }

    private void handleUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        writeSecurityError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录或重新登录");
    }

    private void handleForbidden(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        writeSecurityError(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "当前账户无权执行此操作");
    }

    private void writeSecurityError(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(code, message, null));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(webProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
