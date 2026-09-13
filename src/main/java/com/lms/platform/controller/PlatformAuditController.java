package com.lms.platform.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.platform.dto.TenantAuditLogDto;
import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantAuditEvent;
import com.lms.platform.repository.TenantAuditEventRepository;
import com.lms.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiPaths.PLATFORM + "/audit-logs")
@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class PlatformAuditController {

    private final TenantAuditEventRepository auditRepository;
    private final TenantRepository tenantRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantAuditLogDto>>> getAuditLogs(
            @RequestParam(required = false) UUID tenantId) {

        Map<UUID, Tenant> tenantMap = tenantRepository.findAll().stream()
                .collect(Collectors.toMap(Tenant::getId, Function.identity(), (a, b) -> a));

        List<TenantAuditEvent> events = tenantId != null
                ? auditRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                : auditRepository.findAllByOrderByCreatedAtDesc();

        List<TenantAuditLogDto> dtos = events.stream().map(event -> {
            Tenant t = tenantMap.get(event.getTenantId());
            String slug = t != null ? t.getSlug() : "unknown";
            String name = t != null ? t.getName() : "Unknown Tenant";
            return TenantAuditLogDto.from(event, slug, name);
        }).toList();

        return ResponseEntity.ok(ApiResponse.of(dtos));
    }
}
