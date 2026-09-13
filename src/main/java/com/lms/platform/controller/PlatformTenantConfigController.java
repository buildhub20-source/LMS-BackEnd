package com.lms.platform.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.platform.dto.TenantConfigDto;
import com.lms.platform.dto.UpdateTenantConfigRequest;
import com.lms.platform.security.PlatformAdminPrincipal;
import com.lms.platform.service.TenantConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.PLATFORM + "/tenants")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class PlatformTenantConfigController {

    private final TenantConfigService configService;

    @GetMapping("/{tenantId}/config")
    public ResponseEntity<ApiResponse<TenantConfigDto>> getConfig(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.of(configService.getConfig(tenantId)));
    }

    @PutMapping("/{tenantId}/config")
    public ResponseEntity<ApiResponse<TenantConfigDto>> updateConfig(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantConfigRequest request,
            @AuthenticationPrincipal PlatformAdminPrincipal admin) {
        return ResponseEntity.ok(ApiResponse.of(configService.updateConfig(tenantId, request, admin.id())));
    }
}
