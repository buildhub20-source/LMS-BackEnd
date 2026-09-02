package com.lms.platform.runtime;

import com.lms.common.constants.ApiPaths;
import com.lms.config.PlatformConfig;
import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantStatus;
import com.lms.platform.repository.TenantRepository;
import com.lms.platform.service.TenantSecretCipher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/** Resolves a tenant before any authentication or JPA access occurs. */
@Component
public class TenantContextFilter extends OncePerRequestFilter {
    private final PlatformConfig config;
    private final TenantRepository tenantRepository;
    private final TenantSecretCipher cipher;

    public TenantContextFilter(PlatformConfig config, TenantRepository tenantRepository, TenantSecretCipher cipher) {
        this.config = config;
        this.tenantRepository = tenantRepository;
        this.cipher = cipher;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ApiPaths.API_V1)
                || request.getRequestURI().startsWith(ApiPaths.PLATFORM)
                || request.getRequestURI().startsWith(ApiPaths.WELL_KNOWN);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!config.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        String slug = resolveSlug(request);
        if (slug == null) {
            if (config.isTenantResolutionRequired()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A tenant must be selected");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }
        Tenant tenant = tenantRepository.findBySlug(slug).orElse(null);
        if (tenant == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Tenant not found");
            return;
        }
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant is not active");
            return;
        }
        TenantContext.set(new TenantConnection(tenant.getId(), tenant.getSlug(), tenant.getJdbcUrl(),
                tenant.getDatabaseUsername(), cipher.decrypt(tenant.getEncryptedDatabasePassword())));
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveSlug(HttpServletRequest request) {
        String header = request.getHeader(config.getTenantHeader());
        if (header != null && !header.isBlank()) return header.trim().toLowerCase(Locale.ROOT);
        String baseDomain = config.getBaseDomain();
        String host = request.getServerName().toLowerCase(Locale.ROOT);
        if (baseDomain != null && !baseDomain.isBlank() && host.endsWith("." + baseDomain.toLowerCase(Locale.ROOT))) {
            String prefix = host.substring(0, host.length() - baseDomain.length() - 1);
            return prefix.contains(".") || prefix.isBlank() ? null : prefix;
        }
        return null;
    }
}
