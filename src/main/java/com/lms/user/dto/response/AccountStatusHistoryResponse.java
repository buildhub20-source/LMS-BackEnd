package com.lms.user.dto.response;

import com.lms.user.entity.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public class AccountStatusHistoryResponse {

    private UUID id;

    private AccountStatus status;

    private UUID changedBy;

    private String reason;

    private Instant changedAt;

    public AccountStatusHistoryResponse() {
    }

    public AccountStatusHistoryResponse(UUID id, AccountStatus status, UUID changedBy, String reason, Instant changedAt) {
        this.id = id;
        this.status = status;
        this.changedBy = changedBy;
        this.reason = reason;
        this.changedAt = changedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public UUID getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(UUID changedBy) {
        this.changedBy = changedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }
}
