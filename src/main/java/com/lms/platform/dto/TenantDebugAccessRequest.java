package com.lms.platform.dto;

public record TenantDebugAccessRequest(
        String reason,
        Integer durationMinutes
) {
    public String resolveReason() {
        return (reason != null && !reason.isBlank()) ? reason.trim() : "Bug Analysis & Diagnostics";
    }

    public int resolveDurationMinutes() {
        return (durationMinutes != null && durationMinutes > 0 && durationMinutes <= 60) ? durationMinutes : 30;
    }
}
