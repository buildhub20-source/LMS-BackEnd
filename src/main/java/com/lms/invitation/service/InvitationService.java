package com.lms.invitation.service;

import com.lms.common.response.PageResponse;
import com.lms.invitation.dto.request.CreateInvitationRequest;
import com.lms.invitation.dto.response.InvitationResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Invitation lifecycle.
 *
 * <p>This is also how user accounts come into existence: creating an invitation
 * creates the {@code users} row with a temporary password and emails it. The
 * row stays pending until the user replaces that password, which is what marks
 * the account as onboarded.
 */
public interface InvitationService {

    InvitationResponse invite(CreateInvitationRequest request);

    PageResponse<InvitationResponse> findAll(Pageable pageable);

    InvitationResponse findById(UUID id);

    /** Issues a fresh token, invalidating the previous one, and re-sends it. */
    InvitationResponse resend(UUID id);

    /** Withdraws an unaccepted invitation and the account created alongside it. */
    void revoke(UUID id);
}
