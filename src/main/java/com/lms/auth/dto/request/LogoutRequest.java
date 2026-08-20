package com.lms.auth.dto.request;

/**
 * Logout payload.
 *
 * <p>The refresh token is optional: when it is absent the session identified by
 * the {@code sid} claim of the access token is revoked instead, so a client
 * that has already discarded its refresh token can still sign out cleanly.
 */
public class LogoutRequest {

    private String refreshToken;

    public LogoutRequest() {
    }

    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
