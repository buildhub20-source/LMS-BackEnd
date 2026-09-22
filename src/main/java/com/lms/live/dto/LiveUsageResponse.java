package com.lms.live.dto;

import java.util.UUID;

public record LiveUsageResponse(
        UUID tenantId,
        String billingMonth,
        long participantMinutesUsed,
        int monthlyLiveParticipantMinutesLimit,
        long remainingMinutes,
        long recordingMinutesUsed,
        int monthlyRecordingMinutesLimit,
        int sessionsHosted,
        int peakConcurrentParticipants
) {
    public static LiveUsageResponse of(
            UUID tenantId,
            String billingMonth,
            long participantMinutesUsed,
            int monthlyLimit,
            long recordingMinutesUsed,
            int recordingLimit,
            int sessionsHosted,
            int peakConcurrent
    ) {
        long remaining = Math.max(0, (long) monthlyLimit - participantMinutesUsed);
        return new LiveUsageResponse(
                tenantId,
                billingMonth,
                participantMinutesUsed,
                monthlyLimit,
                remaining,
                recordingMinutesUsed,
                recordingLimit,
                sessionsHosted,
                peakConcurrent
        );
    }
}
