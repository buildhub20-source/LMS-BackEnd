package com.lms.platform.service;

import com.lms.assessment.repository.AssessmentRepository;
import com.lms.course.repository.CourseRepository;
import com.lms.platform.dto.PlatformOverviewResponse;
import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantStatus;
import com.lms.platform.repository.TenantRepository;
import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformOverviewService {

    private final TenantRepository tenantRepository;
    private final TenantSecretCipher cipher;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AssessmentRepository assessmentRepository;

    public PlatformOverviewResponse getOverview() {
        List<Tenant> tenants = tenantRepository.findAll();

        long totalTenants = tenants.size();
        long activeTenants = tenants.stream().filter(t -> t.getStatus() == TenantStatus.ACTIVE).count();
        long suspendedTenants = tenants.stream().filter(t -> t.getStatus() == TenantStatus.SUSPENDED).count();
        long provisioningTenants = tenants.stream().filter(t -> t.getStatus() == TenantStatus.PROVISIONING).count();

        long totalUsers = 0;
        long totalCourses = 0;
        long totalAssessments = 0;

        for (Tenant tenant : tenants) {
            if (tenant.getStatus() != TenantStatus.ACTIVE || tenant.getJdbcUrl() == null) {
                continue;
            }
            try {
                String password = cipher.decrypt(tenant.getEncryptedDatabasePassword());
                TenantConnection connection = new TenantConnection(
                        tenant.getId(),
                        tenant.getSlug(),
                        tenant.getJdbcUrl(),
                        tenant.getDatabaseUsername(),
                        password
                );
                TenantContext.set(connection);
                try {
                    totalUsers += userRepository.count();
                    totalCourses += courseRepository.count();
                    totalAssessments += assessmentRepository.count();
                } finally {
                    TenantContext.clear();
                }
            } catch (Exception ex) {
                log.warn("Could not aggregate metrics for tenant '{}': {}", tenant.getSlug(), ex.getMessage());
            }
        }

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        Map<String, Object> systemHealth = new LinkedHashMap<>();
        systemHealth.put("status", "OPERATIONAL");
        systemHealth.put("controlPlane", "UP");
        systemHealth.put("databaseStatus", "CONNECTED");
        systemHealth.put("usedMemoryMb", usedMemory);
        systemHealth.put("maxMemoryMb", maxMemory);
        systemHealth.put("availableProcessors", runtime.availableProcessors());

        return new PlatformOverviewResponse(
                totalTenants,
                activeTenants,
                suspendedTenants,
                provisioningTenants,
                totalUsers,
                totalCourses,
                totalAssessments,
                systemHealth
        );
    }
}
