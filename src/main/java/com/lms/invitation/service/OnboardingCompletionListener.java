package com.lms.invitation.service;

import com.lms.invitation.entity.Invitation;
import com.lms.invitation.repository.InvitationRepository;
import com.lms.user.event.PasswordChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Closes out onboarding when a user replaces the temporary password they were
 * issued.
 *
 * <p>A pending invitation is what marks an account as still holding a temporary
 * credential, so stamping {@code accepted_at} here is what lifts the
 * password-change requirement. Runs synchronously inside the caller
 * transaction, so the password change and the stamp commit together.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnboardingCompletionListener {

    private final InvitationRepository invitationRepository;

    @EventListener
    @Transactional
    public void onPasswordChanged(PasswordChangedEvent event) {
        invitationRepository.findFirstByUserIdAndAcceptedAtIsNullOrderByCreatedAtDesc(event.getUserId())
                .ifPresent(this::complete);
    }

    private void complete(Invitation invitation) {
        invitation.setAcceptedAt(Instant.now());
        log.info("Onboarding completed for {}", invitation.getUser().getEmail());
    }
}
