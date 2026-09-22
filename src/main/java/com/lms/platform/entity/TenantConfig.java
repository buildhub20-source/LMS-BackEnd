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

    @Column(name = "chat_file_retention_days", nullable = false)
    @Builder.Default
    private Integer chatFileRetentionDays = 30;

    // Live session fields
    @Column(name = "live_classes_enabled", nullable = false)
    @Builder.Default
    private boolean liveClassesEnabled = false;

    @Column(name = "max_live_participants", nullable = false)
    @Builder.Default
    private Integer maxLiveParticipants = 50;

    @Column(name = "monthly_live_participant_minutes", nullable = false)
    @Builder.Default
    private Integer monthlyLiveParticipantMinutes = 1000;

    @Column(name = "max_live_session_duration_minutes", nullable = false)
    @Builder.Default
    private Integer maxLiveSessionDurationMinutes = 120;

    @Column(name = "max_concurrent_live_sessions", nullable = false)
    @Builder.Default
    private Integer maxConcurrentLiveSessions = 3;

    @Column(name = "recording_enabled", nullable = false)
    @Builder.Default
    private boolean recordingEnabled = false;

    @Column(name = "monthly_recording_minutes", nullable = false)
    @Builder.Default
    private Integer monthlyRecordingMinutes = 500;

    @Column(name = "attendance_enabled", nullable = false)
    @Builder.Default
    private boolean attendanceEnabled = true;

    @Column(name = "live_chat_enabled", nullable = false)
    @Builder.Default
    private boolean liveChatEnabled = true;

    @Column(name = "screen_share_enabled", nullable = false)
    @Builder.Default
    private boolean screenShareEnabled = true;

    @Column(name = "ai_transcript_enabled", nullable = false)
    @Builder.Default
    private boolean aiTranscriptEnabled = false;

    @Column(name = "ai_summary_enabled", nullable = false)
    @Builder.Default
    private boolean aiSummaryEnabled = false;
}
