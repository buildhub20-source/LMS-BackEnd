package com.lms.live.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.live.service.GoogleCalendarService;
import com.lms.security.authentication.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import com.lms.platform.repository.TenantConfigRepository;
import com.lms.platform.runtime.TenantContext;
import com.lms.platform.runtime.TenantSlug;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/live-sessions/google")
@RequiredArgsConstructor
public class GoogleCalendarController {
    static final String LIVE_CLASSES_PATH = "/admin/live-classes";
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
        UUID adminId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(Map.of("authorizationUrl", googleCalendarService.authorizationUrl(adminId))));
    }

    public record CompleteConnectionRequest(@NotBlank String code, @NotBlank String state) {}

    /**
     * Second half of the connection, made by the signed-in admin's browser. The
     * state must have been issued to this admin in this institute, so a consent
     * link forwarded to anyone else cannot connect their Google account.
     */
    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> complete(@Valid @RequestBody CompleteConnectionRequest request) {
        UUID adminId = AuthenticationService.requirePrincipal().getUserId();
        googleCalendarService.completeConnection(request.code(), request.state(), adminId);
        return ResponseEntity.ok(ApiResponse.of(Map.of("status", "connected")));
    }

    /**
     * Google redirects here (public). Nothing is connected at this point: the code
     * and state are handed to the institute's Live Classes page in the URL fragment
     * (never sent to a server or in a Referer), and that page finishes the
     * connection through the signed-in {@code /complete} call.
     */
    @GetMapping("/callback")
    public void callback(@RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @RequestParam(required = false) String error,
                         HttpServletResponse response) throws IOException {
        String base = platformConfig.getGoogleCalendar().getFrontendBaseUrl();
        String slug;
        try {
            slug = state == null ? null : googleCalendarService.tenantSlugFromState(state);
        } catch (RuntimeException ex) {
            slug = null;
        }
        if (slug == null) {
            response.sendRedirect(base + LIVE_CLASSES_PATH + "#googleMeet=error");
            return;
        }
        String target = tenantFrontendUrl(base, platformConfig.getBaseDomain(), slug) + LIVE_CLASSES_PATH;
        if (error != null || code == null) {
            response.sendRedirect(target + "#googleMeet=error");
            return;
        }
        response.sendRedirect(target + "#googleCode=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&googleState=" + URLEncoder.encode(state, StandardCharsets.UTF_8));
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
