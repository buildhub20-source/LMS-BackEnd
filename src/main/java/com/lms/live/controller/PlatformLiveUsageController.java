package com.lms.live.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.live.dto.LiveUsageResponse;
import com.lms.live.dto.UpdateLiveConfigRequest;
import com.lms.live.service.LiveUsageService;
import com.lms.platform.dto.TenantConfigDto;
import com.lms.platform.dto.UpdateTenantConfigRequest;
import com.lms.platform.security.PlatformAdminPrincipal;
import com.lms.platform.service.TenantConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.PLATFORM + "/tenants/{tenantId}")
@RequiredArgsConstructor
public class PlatformLiveUsageController {

    private final LiveUsageService liveUsageService;
    private final TenantConfigService tenantConfigService;

    @GetMapping("/live-usage")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN') or hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LiveUsageResponse>> getLiveUsage(@PathVariable UUID tenantId) {
        LiveUsageResponse usage = liveUsageService.getUsage(tenantId);
        return ResponseEntity.ok(ApiResponse.of(usage));
    }

    @PutMapping("/live-config")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<TenantConfigDto>> updateLiveConfig(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateLiveConfigRequest request,
            @AuthenticationPrincipal PlatformAdminPrincipal admin
    ) {
        TenantConfigDto current = tenantConfigService.getConfig(tenantId);
        UpdateTenantConfigRequest fullUpdate = new UpdateTenantConfigRequest(
                current.maxUsers(),
                current.maxCourses(),
                current.maxStorageGb(),
                current.aiFeaturesEnabled(),
                current.advancedAnalyticsEnabled(),
                current.customCertificatesEnabled(),
                current.codeEvaluatorEnabled(),
                current.liveProctoringEnabled(),
                current.chatFileRetentionDays(),
                request.liveClassesEnabled(),
                request.maxLiveParticipants(),
                request.monthlyLiveParticipantMinutes(),
                request.maxLiveSessionDurationMinutes(),
                request.maxConcurrentLiveSessions(),
                request.recordingEnabled(),
                request.monthlyRecordingMinutes(),
                request.attendanceEnabled(),
                request.liveChatEnabled(),
                request.screenShareEnabled(),
                request.aiTranscriptEnabled(),
                request.aiSummaryEnabled()
        );
        UUID adminId = admin != null ? admin.id() : null;
        TenantConfigDto updated = tenantConfigService.updateConfig(tenantId, fullUpdate, adminId);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }
}
