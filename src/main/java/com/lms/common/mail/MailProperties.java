package com.lms.common.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Outbound mail settings. */
@Getter
@Setter
@ConfigurationProperties(prefix = "lms.mail")
public class MailProperties {

    /** When false, messages are logged instead of sent. */
    private boolean enabled = false;

    private String from = "no-reply@lms.local";

    private String fromName = "LMS";
}
