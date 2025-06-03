package com.sopuro.auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {
    private String accessSecret;
    private String refreshSecret;
    private long accessExpirationMs;
    private long refreshExpirationMs;
}
