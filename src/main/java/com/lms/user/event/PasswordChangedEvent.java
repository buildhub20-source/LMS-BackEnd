package com.lms.user.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Raised whenever a user replaces their own password, whether through the
 * change-password endpoint or a reset link.
 *
 * <p>An event rather than a direct call because the invitation module needs to
 * react to this to close out onboarding, and a direct call would make the user
 * and invitation modules depend on each other in both directions.
 *
 * <p>Value equality is required: tests assert the published event equals an
 * expected instance.
 */
public class PasswordChangedEvent {

    private UUID userId;

    public PasswordChangedEvent() {
    }

    public PasswordChangedEvent(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PasswordChangedEvent event)) {
            return false;
        }
        return Objects.equals(userId, event.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
