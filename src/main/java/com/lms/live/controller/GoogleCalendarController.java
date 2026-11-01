package com.lms.live.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.live.service.GoogleCalendarService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.lms.platform.repository.TenantConfigRepository;
import com.lms.platform.runtime.TenantContext;
import com.lms.platform.runtime.TenantSlug;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/live-sessions/google")
@RequiredArgsConstructor
public class GoogleCalendarController {
    private final GoogleCalendarService googleCalendarService;
    private final com.lms.config.PlatformConfig platformConfig;
    private final com.lms.platform.service.CentralTenantConfigReader centralTenantConfigReader;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        var tenantId = TenantContext.current().map(t -> t.tenantId()).orElse(null);
        var config = tenantId == null ? null : centralTenantConfigReader.find(tenantId);
        return ResponseEntity.ok(ApiResponse.of(Map.of(
                "provider", config == null || config.getLiveSessionProvider() == null ? "LIVEKIT" : config.getLiveSessionProvider(),
                "googleCalendarConnected", config != null && config.getGoogleCalendarRefreshToken() != null && !config.getGoogleCalendarRefreshToken().isBlank(),
                "googleCalendarEmail", config == null || config.getGoogleCalendarEmail() == null ? "" : config.getGoogleCalendarEmail())));
    }

    @GetMapping("/connect")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> connect() {
        return ResponseEntity.ok(ApiResponse.of(Map.of("authorizationUrl", googleCalendarService.authorizationUrl())));
    }

    @GetMapping("/callback")
    public void callback(@RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @RequestParam(required = false) String error,
                         HttpServletResponse response) throws IOException {
        String base = platformConfig.getGoogleCalendar().getFrontendBaseUrl();
        if (error != null || code == null || state == null) {
            response.sendRedirect(base + "/admin/analytics?googleMeet=error");
            return;
        }
        try {
            String slug = googleCalendarService.handleCallback(code, state);
            response.sendRedirect(tenantFrontendUrl(base, platformConfig.getBaseDomain(), slug) + "/admin/analytics?googleMeet=connected");
        } catch (RuntimeException ex) {
            response.sendRedirect(base + "/admin/analytics?googleMeet=error&message=" +
                    URLEncoder.encode(ex.getMessage() == null ? "connection_failed" : ex.getMessage(), StandardCharsets.UTF_8));
        }
    }

    static String tenantFrontendUrl(String base, String configuredDomain, String slug) {
        URI uri = URI.create(base);
        String host = uri.getHost();
        if (host == null || !TenantSlug.isWellFormed(slug)) {
            throw new IllegalArgumentException("Invalid tenant redirect configuration");
        }
        String domain = configuredDomain == null || configuredDomain.isBlank() ? host : configuredDomain;
        if (!host.equalsIgnoreCase(domain) && !host.equalsIgnoreCase(slug + "." + domain)) {
            throw new IllegalArgumentException("Frontend host does not match the tenant base domain");
        }
        try {
            return new URI(uri.getScheme(), null, slug + "." + domain, uri.getPort(), uri.getPath(), null, null).toString().replaceAll("/$", "");
        } catch (java.net.URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid tenant redirect configuration", ex);
        }
    }
}
