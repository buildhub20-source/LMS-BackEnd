package com.lms.organization.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.organization.entity.OrganizationSettings;
import com.lms.organization.repository.OrganizationSettingsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "Organization Settings")
@RestController
@RequestMapping(ApiPaths.ORGANIZATION)
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationSettingsRepository orgSettingsRepository;

    @Operation(summary = "Get organization settings and branding tokens")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        OrganizationSettings settings = orgSettingsRepository.findFirstBy()
                .orElseGet(() -> OrganizationSettings.builder()
                        .name("Acme Learning Academy")
                        .domain("lms.local")
                        .supportEmail("support@acme.edu")
                        .primaryColor("#10b981")
                        .build());

        Map<String, Object> response = new HashMap<>();
        response.put("id", settings.getId());
        response.put("name", settings.getName());
        response.put("portalTitle", settings.getName());
        response.put("domain", settings.getDomain());
        response.put("supportEmail", settings.getSupportEmail());
        response.put("description", settings.getDescription());
        response.put("primaryColor", settings.getPrimaryColor() != null ? settings.getPrimaryColor() : "#10b981");
        response.put("logoUrl", settings.getLogoUrl());

        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @Operation(summary = "Update organization settings or branding")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSettings(@RequestBody Map<String, Object> payload) {
        OrganizationSettings settings = orgSettingsRepository.findFirstBy()
                .orElseGet(() -> OrganizationSettings.builder()
                        .name("Acme Learning Academy")
                        .domain("lms.local")
                        .supportEmail("support@acme.edu")
                        .primaryColor("#10b981")
                        .build());

        // Extract branding fields if nested or flat
        if (payload.containsKey("portalTitle") && payload.get("portalTitle") != null) {
            settings.setName(payload.get("portalTitle").toString().trim());
        } else if (payload.containsKey("name") && payload.get("name") != null) {
            settings.setName(payload.get("name").toString().trim());
        }

        if (payload.containsKey("primaryColor") && payload.get("primaryColor") != null) {
            settings.setPrimaryColor(payload.get("primaryColor").toString().trim());
        }

        if (payload.containsKey("logoUrl")) {
            Object logo = payload.get("logoUrl");
            settings.setLogoUrl(logo != null ? logo.toString().trim() : null);
        }

        if (payload.containsKey("supportEmail") && payload.get("supportEmail") != null) {
            settings.setSupportEmail(payload.get("supportEmail").toString().trim());
        }

        if (payload.containsKey("customDomain") && payload.get("customDomain") != null) {
            settings.setDomain(payload.get("customDomain").toString().trim());
        } else if (payload.containsKey("domain") && payload.get("domain") != null) {
            settings.setDomain(payload.get("domain").toString().trim());
        }

        // Check for nested general map
        if (payload.get("general") instanceof Map<?, ?> generalMap) {
            if (generalMap.containsKey("name") && generalMap.get("name") != null) {
                settings.setName(generalMap.get("name").toString().trim());
            }
            if (generalMap.containsKey("supportEmail") && generalMap.get("supportEmail") != null) {
                settings.setSupportEmail(generalMap.get("supportEmail").toString().trim());
            }
            if (generalMap.containsKey("customDomain") && generalMap.get("customDomain") != null) {
                settings.setDomain(generalMap.get("customDomain").toString().trim());
            }
        }

        OrganizationSettings saved = orgSettingsRepository.save(settings);
        log.info("Updated organization settings: name={}, primaryColor={}", saved.getName(), saved.getPrimaryColor());

        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("name", saved.getName());
        response.put("portalTitle", saved.getName());
        response.put("domain", saved.getDomain());
        response.put("supportEmail", saved.getSupportEmail());
        response.put("primaryColor", saved.getPrimaryColor());
        response.put("logoUrl", saved.getLogoUrl());

        return ResponseEntity.ok(ApiResponse.of(response, "Organization settings updated successfully"));
    }
}
