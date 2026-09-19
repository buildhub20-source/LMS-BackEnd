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
        @Min(1) Integer chatFileRetentionDays
) {}
