package com.lms.invitation.dto.request;

import com.lms.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public class AcceptInvitationRequest {

    @NotBlank
    private String token;

    @NotBlank
    @StrongPassword
    private String password;

    public AcceptInvitationRequest() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
