package com.lms.live.dto;

import jakarta.validation.constraints.Min;

public record UpdateLiveConfigRequest(
        Boolean liveClassesEnabled,
        @Min(1) Integer maxLiveParticipants,
        @Min(1) Integer monthlyLiveParticipantMinutes,
        @Min(1) Integer maxLiveSessionDurationMinutes,
        @Min(1) Integer maxConcurrentLiveSessions,
        Boolean recordingEnabled,
        @Min(1) Integer monthlyRecordingMinutes,
        Boolean attendanceEnabled,
        Boolean liveChatEnabled,
        Boolean screenShareEnabled,
        Boolean aiTranscriptEnabled,
        Boolean aiSummaryEnabled
) {}
