package com.lms.platform.dto;

import com.lms.platform.entity.TenantConfig;

import java.time.Instant;
import java.util.UUID;

public record TenantConfigDto(
        UUID id,
        UUID tenantId,
        Integer maxUsers,
        Integer maxCourses,
        Integer maxStorageGb,
        boolean aiFeaturesEnabled,
        boolean advancedAnalyticsEnabled,
        boolean customCertificatesEnabled,
        boolean codeEvaluatorEnabled,
        boolean liveProctoringEnabled,
        Integer chatFileRetentionDays,
        Instant updatedAt
) {
    public static TenantConfigDto from(TenantConfig config) {
        return new TenantConfigDto(
                config.getId(),
                config.getTenantId(),
                config.getMaxUsers(),
                config.getMaxCourses(),
                config.getMaxStorageGb(),
                config.isAiFeaturesEnabled(),
                config.isAdvancedAnalyticsEnabled(),
                config.isCustomCertificatesEnabled(),
                config.isCodeEvaluatorEnabled(),
                config.isLiveProctoringEnabled(),
                config.getChatFileRetentionDays() != null ? config.getChatFileRetentionDays() : 30,
                config.getUpdatedAt()
        );
    }
}
