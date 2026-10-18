package com.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "lms.livekit")
public class LiveKitConfig {
    private String url;
    private String apiKey;
    private String apiSecret;
    private int tokenTtlMinutes = 120;
}
