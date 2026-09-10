package com.lms.enrollment.event;

import com.lms.common.client.CertificateWebhookClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Delivers the certificate notification only after the enrollment commit succeeds. */
@Component
@RequiredArgsConstructor
public class EnrollmentCompletedEventListener {

    private final CertificateWebhookClient certificateWebhookClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEnrollmentCompleted(EnrollmentCompletedEvent event) {
        certificateWebhookClient.notifyEnrollmentCompleted(
                event.studentId(), event.courseId(), event.tenantSlug());
    }
}
