package com.lms.platform.entity;

import com.lms.common.audit.Timestamped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(schema = "platform", name = "tenant_configs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfig extends Timestamped {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "max_users", nullable = false)
    @Builder.Default
    private Integer maxUsers = 500;

    @Column(name = "max_courses", nullable = false)
    @Builder.Default
    private Integer maxCourses = 50;

    @Column(name = "max_storage_gb", nullable = false)
    @Builder.Default
    private Integer maxStorageGb = 20;

    @Column(name = "ai_features_enabled", nullable = false)
    @Builder.Default
    private boolean aiFeaturesEnabled = true;

    @Column(name = "advanced_analytics_enabled", nullable = false)
    @Builder.Default
    private boolean advancedAnalyticsEnabled = true;

    @Column(name = "custom_certificates_enabled", nullable = false)
    @Builder.Default
    private boolean customCertificatesEnabled = true;

    @Column(name = "code_evaluator_enabled", nullable = false)
    @Builder.Default
    private boolean codeEvaluatorEnabled = true;

    @Column(name = "live_proctoring_enabled", nullable = false)
    @Builder.Default
    private boolean liveProctoringEnabled = false;
}
