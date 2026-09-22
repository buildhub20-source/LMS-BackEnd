package com.lms.live.entity;

import com.lms.common.audit.Timestamped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tenant_live_usage", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_live_usage_month", columnNames = {"tenant_id", "billing_month"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantLiveUsage extends Timestamped {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    @Column(name = "participant_minutes_used", nullable = false)
    @Builder.Default
    private long participantMinutesUsed = 0L;

    @Column(name = "recording_minutes_used", nullable = false)
    @Builder.Default
    private long recordingMinutesUsed = 0L;

    @Column(name = "sessions_hosted", nullable = false)
    @Builder.Default
    private int sessionsHosted = 0;

    @Column(name = "peak_concurrent_participants", nullable = false)
    @Builder.Default
    private int peakConcurrentParticipants = 0;
}
