package com.lms.invitation.service;

import com.lms.common.response.PageResponse;
import com.lms.invitation.dto.request.CreateInvitationRequest;
import com.lms.invitation.dto.response.InvitationResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Invitation lifecycle.
 *
 * <p>Creating an invitation creates the {@code users} row with a null password
 * and {@code is_active = false}, then emails a secure link. The user sets their
 * own password by redeeming that link, which activates the account.
 */
public interface InvitationService {

    InvitationResponse invite(CreateInvitationRequest request);

    PageResponse<InvitationResponse> findAll(Pageable pageable);

    InvitationResponse findById(UUID id);

    /** Issues a fresh token, invalidating the previous one, and re-sends the link. */
    InvitationResponse resend(UUID id);

    /** Withdraws an unaccepted invitation and the account created alongside it. */
    void revoke(UUID id);

    /** Validates the token, sets the user's password, and marks the invitation accepted. */
    void acceptInvitation(String rawToken, String newPassword);
}
