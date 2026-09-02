package com.lms.common.dto.internal;

/**
 * Minimal org-settings projection returned by the internal API for the cert service.
 * Used by the cert service to brand the generated PDF certificate.
 */
public record InternalOrgSettingsDto(
        String name,
        String domain,
        String logoUrl,
        String primaryColor,
        String supportEmail
) {}
