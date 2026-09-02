package com.lms.platform.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.platform.dto.CreateTenantRequest;
import com.lms.platform.dto.TenantResponse;
import com.lms.platform.security.PlatformAdminPrincipal;
import com.lms.platform.service.TenantProvisioningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Tenant lifecycle endpoints, available only to the global platform admin. */
@RestController
@RequestMapping(ApiPaths.PLATFORM + "/tenants")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
public class TenantManagementController {
    private final TenantProvisioningService service;
    public TenantManagementController(TenantProvisioningService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.of(service.list()));
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> get(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.of(service.getResponse(tenantId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> create(@Valid @RequestBody CreateTenantRequest request,
                                                               @AuthenticationPrincipal PlatformAdminPrincipal admin) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.of(service.create(request, admin.id())));
    }

    @PostMapping("/{tenantId}/provision")
    public ResponseEntity<ApiResponse<TenantResponse>> provision(@PathVariable UUID tenantId,
                                                                  @AuthenticationPrincipal PlatformAdminPrincipal admin) {
        return ResponseEntity.ok(ApiResponse.of(service.progress(tenantId, admin.id())));
    }

    @PostMapping("/{tenantId}/suspend")
    public ResponseEntity<ApiResponse<TenantResponse>> suspend(@PathVariable UUID tenantId,
                                                                @AuthenticationPrincipal PlatformAdminPrincipal admin) {
        return ResponseEntity.ok(ApiResponse.of(service.suspend(tenantId, admin.id())));
    }

    @PostMapping("/{tenantId}/pause-cloud")
    public ResponseEntity<ApiResponse<TenantResponse>> pauseCloud(@PathVariable UUID tenantId,
                                                                   @AuthenticationPrincipal PlatformAdminPrincipal admin) {
        return ResponseEntity.ok(ApiResponse.of(service.pauseCloudProject(tenantId, admin.id())));
    }

    @PostMapping("/{tenantId}/restore-cloud")
    public ResponseEntity<ApiResponse<TenantResponse>> restoreCloud(@PathVariable UUID tenantId,
                                                                     @AuthenticationPrincipal PlatformAdminPrincipal admin) {
        return ResponseEntity.ok(ApiResponse.of(service.restoreCloudProject(tenantId, admin.id())));
    }

    @PostMapping("/{tenantId}/schedule-deletion")
    public ResponseEntity<ApiResponse<TenantResponse>> scheduleDeletion(@PathVariable UUID tenantId,
                                                                         @AuthenticationPrincipal PlatformAdminPrincipal admin) {
        return ResponseEntity.ok(ApiResponse.of(service.scheduleDeletion(tenantId, admin.id())));
    }
}
