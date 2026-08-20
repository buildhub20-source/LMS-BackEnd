package com.lms.user.dto.request;

import jakarta.validation.constraints.Size;

/** Reason recorded in account_status_history for a lock or unlock. */
public class LockUserRequest {

    @Size(max = 255)
    private String reason;

    public LockUserRequest() {
    }

    public LockUserRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
