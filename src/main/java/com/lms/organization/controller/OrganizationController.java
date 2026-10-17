package com.lms.organization.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.organization.dto.UpdateOrganizationSettingsRequest;
import com.lms.organization.entity.OrganizationSettings;
import com.lms.organization.service.OrganizationSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "Organization Settings")
@RestController
@RequestMapping(ApiPaths.ORGANIZATION)
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationSettingsService settingsService;

    @Operation(summary = "Get organization settings and branding tokens")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        return ResponseEntity.ok(ApiResponse.of(toResponse(settingsService.current())));
    }

    @Operation(summary = "Update organization settings or branding")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSettings(
            @Valid @RequestBody UpdateOrganizationSettingsRequest request) {
        OrganizationSettings saved = settingsService.update(request);
        log.info("Updated organization settings: name={}", saved.getName());
        return ResponseEntity.ok(ApiResponse.of(toResponse(saved), "Organization settings updated successfully"));
    }

    private Map<String, Object> toResponse(OrganizationSettings settings) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", settings.getId());
        response.put("name", settings.getName());
        response.put("portalTitle", settings.getName());
        response.put("domain", settings.getDomain());
        response.put("supportEmail", settings.getSupportEmail());
        response.put("description", settings.getDescription());
        response.put("primaryColor", settings.getPrimaryColor() != null
                ? settings.getPrimaryColor() : OrganizationSettingsService.DEFAULT_PRIMARY_COLOR);
        response.put("logoUrl", settings.getLogoUrl());
        return response;
    }
}
