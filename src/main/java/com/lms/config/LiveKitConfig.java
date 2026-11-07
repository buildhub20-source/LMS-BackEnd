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
    /**
     * Built-in (LiveKit) live classes. Off when the deployment has no LiveKit
     * server: the production secrets check then skips LiveKit, and scheduling
     * a built-in class is refused (Google Meet classes still work).
     */
    private boolean enabled = true;
    private String url;
    private String apiKey;
    private String apiSecret;
    private int tokenTtlMinutes = 120;
}
