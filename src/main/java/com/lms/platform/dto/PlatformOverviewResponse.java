package com.lms.platform.dto;

import java.util.Map;

public record PlatformOverviewResponse(
        long totalTenants,
        long activeTenants,
        long suspendedTenants,
        long provisioningTenants,
        long totalUsers,
        long totalCourses,
        long totalAssessments,
        Map<String, Object> systemHealth
) {}
