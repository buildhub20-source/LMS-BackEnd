package com.lms.platform.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTenantConfigRequest(
        @NotNull @Min(1) Integer maxUsers,
        @NotNull @Min(1) Integer maxCourses,
        @NotNull @Min(1) Integer maxStorageGb,
        boolean aiFeaturesEnabled,
        boolean advancedAnalyticsEnabled,
        boolean customCertificatesEnabled,
        boolean codeEvaluatorEnabled,
        boolean liveProctoringEnabled,
        @Min(1) Integer chatFileRetentionDays,
        // Live session fields (all nullable — only updated when present)
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
