package com.lms.auth.service;

import com.lms.auth.entity.UserSession;
import com.lms.user.entity.User;

import java.util.List;
import java.util.UUID;

/**
 * Owns the refresh-token session lifecycle: issue, rotate, revoke.
 *
 * <p>The raw refresh token exists only in the {@link IssuedSession} returned to
 * the caller. Only its digest is persisted.
 */
public interface SessionService {

    /** A newly issued session together with its one-time raw refresh token. */
    class IssuedSession {

        private final UUID sessionId;

        private final String rawRefreshToken;

        public IssuedSession(UUID sessionId, String rawRefreshToken) {
            this.sessionId = sessionId;
            this.rawRefreshToken = rawRefreshToken;
        }

        public UUID getSessionId() {
            return sessionId;
        }

        public String getRawRefreshToken() {
            return rawRefreshToken;
        }
    }

    /** The rotated session plus the replacement raw refresh token. */
    class RotatedSession {

        private final UserSession session;

        private final String rawRefreshToken;

        public RotatedSession(UserSession session, String rawRefreshToken) {
            this.session = session;
            this.rawRefreshToken = rawRefreshToken;
        }

        public UserSession getSession() {
            return session;
        }

        public String getRawRefreshToken() {
            return rawRefreshToken;
        }
    }

    IssuedSession openSession(User user);

    /**
     * Validates a presented refresh token and rotates it.
     *
     * @return the session, now holding the digest of a freshly minted token
     */
    RotatedSession rotate(String rawRefreshToken);

    /** Revokes the session that owns the given refresh token. */
    void revokeByRefreshToken(String rawRefreshToken);

    void revokeById(UUID sessionId, UUID requestingUserId, boolean allowAnyUser);

    int revokeAllForUser(UUID userId);

    List<UserSession> activeSessions(UUID userId);

    /** Deletes sessions whose expiry has passed. */
    int purgeExpired();
}
