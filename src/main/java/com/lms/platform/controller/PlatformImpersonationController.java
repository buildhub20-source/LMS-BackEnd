package com.lms.platform.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.platform.dto.TenantDebugAccessRequest;
import com.lms.platform.dto.TenantImpersonationResponse;
import com.lms.platform.security.PlatformAdminPrincipal;
import com.lms.platform.service.TenantImpersonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.PLATFORM + "/tenants")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class PlatformImpersonationController {

    private final TenantImpersonationService impersonationService;

    @PostMapping("/{tenantId}/impersonate")
    public ResponseEntity<ApiResponse<TenantImpersonationResponse>> impersonate(
            @PathVariable UUID tenantId,
            @RequestBody(required = false) TenantDebugAccessRequest request,
            @AuthenticationPrincipal PlatformAdminPrincipal admin) {
        String reason = (request != null) ? request.resolveReason() : "Bug Analysis & Diagnostics";
        int duration = (request != null) ? request.resolveDurationMinutes() : 30;
        return ResponseEntity.ok(ApiResponse.of(impersonationService.impersonate(tenantId, admin.id(), reason, duration)));
    }
}
