package com.onlinebanking.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class StartupValidation {

    private final Environment environment;

    @PostConstruct
    public void validateRequiredProperties() {
        validatePresent("jwt.secret");
        validatePresent("security.aes.key");
        validatePresent("spring.datasource.url");
        validatePresent("spring.datasource.username");
        validatePresent("spring.datasource.password");
    }

    private void validatePresent(String propertyName) {
        String value = environment.getProperty(propertyName);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Required configuration property is missing or empty: " + propertyName);
        }
    }
}
