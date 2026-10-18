package com.lms.organization.service;

import com.lms.organization.dto.UpdateOrganizationSettingsRequest;
import com.lms.organization.entity.OrganizationSettings;
import com.lms.organization.repository.OrganizationSettingsRepository;
import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Organisation settings for the current tenant. A tenant that has never saved
 * its settings gets defaults built from its own registry entry, never from
 * sample data.
 */
@Service
@RequiredArgsConstructor
public class OrganizationSettingsService {

    public static final String DEFAULT_PRIMARY_COLOR = "#10b981";
    static final String FALLBACK_NAME = "Learning Portal";

    private final OrganizationSettingsRepository repository;

    @Transactional(readOnly = true)
    public OrganizationSettings current() {
        return repository.findFirstBy().orElseGet(this::defaults);
    }

    @Transactional
    public OrganizationSettings update(UpdateOrganizationSettingsRequest request) {
        OrganizationSettings settings = repository.findFirstBy().orElseGet(this::defaults);

        if (request.getName() != null) settings.setName(request.getName().trim());
        if (request.getSupportEmail() != null) settings.setSupportEmail(blankToNull(request.getSupportEmail()));
        if (request.getDescription() != null) settings.setDescription(blankToNull(request.getDescription()));
        if (request.getPrimaryColor() != null) settings.setPrimaryColor(request.getPrimaryColor());
        if (request.getLogoUrl() != null) settings.setLogoUrl(blankToNull(request.getLogoUrl()));

        return repository.save(settings);
    }

    private OrganizationSettings defaults() {
        String tenantName = TenantContext.current()
                .map(TenantConnection::name)
                .filter(name -> !name.isBlank())
                .orElse(FALLBACK_NAME);
        return OrganizationSettings.builder()
                .name(tenantName)
                .primaryColor(DEFAULT_PRIMARY_COLOR)
                .build();
    }

    private static String blankToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
