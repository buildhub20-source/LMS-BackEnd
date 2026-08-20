package com.lms.support;

import com.lms.common.mail.MailMessage;
import com.lms.common.mail.MailSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Captures outbound mail so a test can recover the one-time token that is
 * otherwise only ever present in the message body.
 */
public class RecordingMailSender implements MailSender {

    private static final Pattern TOKEN = Pattern.compile("[?&]token=([A-Za-z0-9_-]+)");

    private final List<MailMessage> sent = new CopyOnWriteArrayList<>();

    @TestConfiguration
    public static class Config {

        @Bean
        @Primary
        public RecordingMailSender recordingMailSender() {
            return new RecordingMailSender();
        }
    }

    @Override
    public void send(MailMessage message) {
        sent.add(message);
    }

    public void clear() {
        sent.clear();
    }

    public List<MailMessage> sent() {
        return List.copyOf(sent);
    }

    public MailMessage lastTo(String recipient) {
        return sent.stream()
                .filter(message -> message.getTo().equalsIgnoreCase(recipient))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("No message was sent to " + recipient));
    }

    /** Extracts the {@code token} query parameter from the link in the message. */
    public String lastTokenFor(String recipient) {
        return tokenIn(lastTo(recipient).getBody())
                .orElseThrow(() -> new AssertionError("No token link in the message to " + recipient));
    }

    private Optional<String> tokenIn(String body) {
        Matcher matcher = TOKEN.matcher(body);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
