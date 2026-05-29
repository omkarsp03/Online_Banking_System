package com.onlinebanking.config;

import com.onlinebanking.util.AttributeEncryptor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, AesProperties.class})
@RequiredArgsConstructor
public class AppConfig {

    private final AesProperties aesProperties;

    @PostConstruct
    public void init() {
        AttributeEncryptor.setSecretKey(aesProperties.getKey());
    }
}
