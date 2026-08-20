package com.lms.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development and test transport. Writes the message to the log so the
 * invitation and reset links are recoverable without an SMTP server.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "lms.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingMailSender implements MailSender {

    @Override
    public void send(MailMessage message) {
        log.info("""
                        
                        ---------------- OUTBOUND MAIL (not sent: lms.mail.enabled=false) ----------------
                        To:      {}
                        Subject: {}
                        
                        {}
                        ---------------------------------------------------------------------------------""",
                message.getTo(), message.getSubject(), message.getBody());
    }
}
