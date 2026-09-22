package com.lms.live.service;

import com.lms.live.dto.LiveUsageResponse;
import com.lms.live.entity.TenantLiveUsage;
import com.lms.live.repository.TenantLiveUsageRepository;
import com.lms.platform.entity.TenantConfig;
import com.lms.platform.repository.TenantConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveUsageService {

    private final TenantLiveUsageRepository usageRepository;
    private final TenantConfigRepository tenantConfigRepository;

    public String getCurrentBillingMonth() {
        return YearMonth.now().toString(); // e.g. "2026-09"
    }

    @Transactional(readOnly = true)
    public LiveUsageResponse getUsage(UUID tenantId) {
        String month = getCurrentBillingMonth();
        TenantLiveUsage usage = getOrCreateUsageRecord(tenantId, month);
        TenantConfig config = tenantConfigRepository.findByTenantId(tenantId).orElse(null);

        int monthlyParticipantLimit = config != null ? config.getMonthlyLiveParticipantMinutes() : 3000;
        int monthlyRecordingLimit = config != null ? config.getMonthlyRecordingMinutes() : 600;

        return LiveUsageResponse.of(
                tenantId,
                month,
                usage.getParticipantMinutesUsed(),
                monthlyParticipantLimit,
                usage.getRecordingMinutesUsed(),
                monthlyRecordingLimit,
                usage.getSessionsHosted(),
                usage.getPeakConcurrentParticipants()
        );
    }

    @Transactional
    public void recordParticipantDuration(UUID tenantId, long durationSeconds) {
        if (tenantId == null || durationSeconds <= 0) return;
        long minutes = Math.max(1, durationSeconds / 60);
        String month = getCurrentBillingMonth();
        TenantLiveUsage usage = getOrCreateUsageRecord(tenantId, month);
        usage.setParticipantMinutesUsed(usage.getParticipantMinutesUsed() + minutes);
        usageRepository.save(usage);
        log.info("Recorded {} participant-minutes for tenant {} (total this month: {})",
                minutes, tenantId, usage.getParticipantMinutesUsed());
    }

    @Transactional
    public void recordSessionHosted(UUID tenantId) {
        if (tenantId == null) return;
        String month = getCurrentBillingMonth();
        TenantLiveUsage usage = getOrCreateUsageRecord(tenantId, month);
        usage.setSessionsHosted(usage.getSessionsHosted() + 1);
        usageRepository.save(usage);
    }

    @Transactional
    public void recordPeakParticipants(UUID tenantId, int currentParticipants) {
        if (tenantId == null || currentParticipants <= 0) return;
        String month = getCurrentBillingMonth();
        TenantLiveUsage usage = getOrCreateUsageRecord(tenantId, month);
        if (currentParticipants > usage.getPeakConcurrentParticipants()) {
            usage.setPeakConcurrentParticipants(currentParticipants);
            usageRepository.save(usage);
        }
    }

    @Transactional(readOnly = true)
    public boolean isQuotaExceeded(UUID tenantId) {
        if (tenantId == null) return false;
        String month = getCurrentBillingMonth();
        TenantLiveUsage usage = getOrCreateUsageRecord(tenantId, month);
        TenantConfig config = tenantConfigRepository.findByTenantId(tenantId).orElse(null);
        if (config == null) return false;

        return usage.getParticipantMinutesUsed() >= config.getMonthlyLiveParticipantMinutes();
    }

    private TenantLiveUsage getOrCreateUsageRecord(UUID tenantId, String month) {
        return usageRepository.findByTenantIdAndBillingMonth(tenantId, month)
                .orElseGet(() -> usageRepository.save(TenantLiveUsage.builder()
                        .tenantId(tenantId)
                        .billingMonth(month)
                        .participantMinutesUsed(0L)
                        .recordingMinutesUsed(0L)
                        .sessionsHosted(0)
                        .peakConcurrentParticipants(0)
                        .build()));
    }
}
