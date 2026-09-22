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
    private String url = "wss://livekit.cloud.local";
    private String apiKey = "devkey";
    private String apiSecret = "secret12345678901234567890123456789012";
    private int tokenTtlMinutes = 120;
}
