package com.lms.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /auth/accept-invitation}.
 *
 * <p>The raw invitation token is sent in the URL of the invitation email.
 * The user sets their permanent password here; the temporary credentials
 * issued at invite time are discarded once this succeeds.
 */
public class AcceptInvitationRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;

    public AcceptInvitationRequest() {
    }

    public AcceptInvitationRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
