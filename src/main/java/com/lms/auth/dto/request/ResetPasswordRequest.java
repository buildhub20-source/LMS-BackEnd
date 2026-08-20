package com.lms.auth.dto.request;

import com.lms.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public class ResetPasswordRequest {

    @NotBlank
    private String token;

    @StrongPassword
    private String newPassword;

    public ResetPasswordRequest() {
    }

    public ResetPasswordRequest(String token, String newPassword) {
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
