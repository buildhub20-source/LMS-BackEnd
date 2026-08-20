package com.lms.common.mail;

/**
 * Transport abstraction for outbound mail.
 *
 * <p>Features own the message copy; this interface only moves it. Kept in
 * {@code common} because both invitation and password reset depend on it.
 */
public interface MailSender {

    void send(MailMessage message);
}
