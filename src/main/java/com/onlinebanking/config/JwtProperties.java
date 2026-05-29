package com.onlinebanking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Data
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank(message = "JWT secret must be provided via environment variable JWT_SECRET")
    private String secret;

    @Positive(message = "JWT expiration must be a positive number of milliseconds")
    private long expirationMs = 3600000L;

    @NotBlank(message = "JWT issuer must be configured")
    private String issuer = "online-banking-app";
}
