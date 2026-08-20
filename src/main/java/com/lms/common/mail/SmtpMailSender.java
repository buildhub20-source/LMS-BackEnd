package com.lms.common.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** SMTP transport, active when {@code lms.mail.enabled} is true. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "lms.mail", name = "enabled", havingValue = "true")
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    @Override
    public void send(MailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(mailProperties.getFrom());
        mail.setTo(message.getTo());
        mail.setSubject(message.getSubject());
        mail.setText(message.getBody());

        javaMailSender.send(mail);
        log.info("Mail sent to {} [{}]", message.getTo(), message.getSubject());
    }
}
