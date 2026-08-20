package com.lms.user.dto.request;

import com.lms.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @StrongPassword
    private String newPassword;

    public ChangePasswordRequest() {
    }

    public ChangePasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
