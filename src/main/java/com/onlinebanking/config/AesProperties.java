package com.onlinebanking.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "security.aes")
public class AesProperties {

    @NotBlank(message = "AES secret must be provided via environment variable AES_SECRET")
    private String key;
}
