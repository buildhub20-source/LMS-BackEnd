package com.lms.invitation.entity;

/**
 * Derived invitation state.
 *
 * <p>Not persisted: the ERD stores {@code accepted_at} and {@code expires_at}
 * and the state is computed from them, so the two can never disagree.
 */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    EXPIRED
}
