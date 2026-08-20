package com.lms.invitation.mapper;

import com.lms.invitation.dto.response.InvitationResponse;
import com.lms.invitation.entity.Invitation;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Hand-written because every field is derived from the associated user or from
 * the accepted/expiry timestamps rather than copied.
 */
@Component
public class InvitationMapper {

    public InvitationResponse toResponse(Invitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getUser().getId(),
                invitation.getUser().getName(),
                invitation.getUser().getEmail(),
                invitation.getUser().roleNames(),
                invitation.status(Instant.now()),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getInvitedBy() == null ? null : invitation.getInvitedBy().getEmail(),
                invitation.getCreatedAt());
    }
}
