package com.lms.live.scheduler;

import com.lms.live.entity.LiveSession;
import com.lms.live.entity.LiveSessionReminder;
import com.lms.live.entity.LiveSessionReminderKind;
import com.lms.live.repository.LiveSessionReminderRepository;
import com.lms.live.repository.LiveSessionRepository;
import com.lms.notification.mail.LearningMailAudience;
import com.lms.notification.mail.LearningMailProperties;
import com.lms.notification.mail.LearningMailService;
import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantStatus;
import com.lms.platform.repository.TenantRepository;
import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import com.lms.platform.service.TenantSecretCipher;
import com.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Sends the two live-class reminder mails: one a day out, one a quarter hour
 * before the class starts.
 *
 * <p>Polls rather than scheduling a timer per session. Timers would be lost on
 * every restart, and this service restarts often; a sweep plus the
 * {@code live_session_reminders} ledger instead means a reminder is sent once
 * and only once, whenever the service happens to be up inside the window. A
 * class whose window passed entirely while the service was down is skipped
 * rather than sent late — a reminder for a class that has already begun is
 * noise, and {@code findScheduledStartingBetween} will not return it.
 *
 * <p>Runs outside any request, so there is no tenant in scope: it walks the
 * active tenants itself and binds each one in turn, the same way
 * {@code PlatformOverviewService} aggregates across tenants.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveSessionReminderScheduler {

    private final TenantRepository tenantRepository;
    private final TenantSecretCipher cipher;
    private final LiveSessionRepository liveSessionRepository;
    private final LiveSessionReminderRepository reminderRepository;
    private final LearningMailService learningMailService;
    private final LearningMailAudience audience;
    private final LearningMailProperties properties;
    private final com.lms.common.sharedstate.ClusterLock clusterLock;

    @Scheduled(fixedDelayString = "${LIVE_REMINDER_POLL_MS:60000}",
            initialDelayString = "${LIVE_REMINDER_INITIAL_DELAY_MS:30000}")
    public void sweep() {
        if (!properties.isLiveReminderEnabled()) {
            return;
        }
        // One backend copy sends reminders per sweep; the reminder table already
        // prevents duplicates, this just avoids the copies racing for them.
        clusterLock.runExclusively("live-session-reminders", java.time.Duration.ofMinutes(5), this::sweepAllTenants);
    }

    private void sweepAllTenants() {

        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getStatus() != TenantStatus.ACTIVE || tenant.getJdbcUrl() == null) {
                continue;
            }
            try {
                TenantContext.set(new TenantConnection(
                        tenant.getId(),
                        tenant.getSlug(),
                        tenant.getName(),
                        tenant.getJdbcUrl(),
                        tenant.getDatabaseUsername(),
                        cipher.decrypt(tenant.getEncryptedDatabasePassword())));
                try {
                    sweepTenant();
                } finally {
                    TenantContext.clear();
                }
            } catch (Exception ex) {
                // One unreachable tenant must not stop the others.
                log.warn("Live reminder sweep failed for tenant '{}': {}", tenant.getSlug(), ex.getMessage());
            }
        }
    }

    private void sweepTenant() {
        Instant now = Instant.now();
        for (LiveSessionReminderKind kind : LiveSessionReminderKind.values()) {
            Instant notBefore = now.plus(kind.minimumLead());
            Instant dueBefore = now.plus(kind.leadTime());
            List<LiveSession> due = liveSessionRepository.findScheduledStartingBetween(notBefore, dueBefore);

            for (LiveSession session : due) {
                if (reminderRepository.existsBySessionIdAndKind(session.getId(), kind)) {
                    continue;
                }
                claimAndSend(session, kind);
            }
        }
    }

    /**
     * Writes the ledger row before mailing, so a crash between the two costs
     * one reminder rather than mailing every student a second time. If another
     * instance claimed it first the unique constraint rejects this row and the
     * send is abandoned.
     */
    private void claimAndSend(LiveSession session, LiveSessionReminderKind kind) {
        List<User> students = audience.enrolledStudents(session.getCourse().getId());

        try {
            reminderRepository.saveAndFlush(LiveSessionReminder.builder()
                    .sessionId(session.getId())
                    .kind(kind)
                    .recipients(students.size())
                    .build());
        } catch (Exception ex) {
            log.debug("Reminder {} for session {} already claimed: {}", kind, session.getId(), ex.getMessage());
            return;
        }

        if (students.isEmpty()) {
            log.info("Live class '{}' has no enrolled students for the {} reminder", session.getTitle(), kind);
            return;
        }

        log.info("Sending {} reminder for live class '{}' to {} student(s)",
                kind, session.getTitle(), students.size());
        learningMailService.sendLiveSessionReminder(
                students, session, session.getCourse().getTitle(), kind);
    }
}
