package com.lms.live.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.common.exception.BusinessRuleException;
import com.lms.config.PlatformConfig;
import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantConfig;
import com.lms.platform.repository.TenantConfigRepository;
import com.lms.platform.repository.TenantRepository;
import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import com.lms.platform.service.TenantSecretCipher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class GoogleCalendarService {
    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.events";
    private final PlatformConfig config;
    private final TenantRepository tenantRepository;
    private final com.lms.platform.service.CentralTenantConfigReader centralTenantConfigReader;
    private final TenantSecretCipher cipher;
    private final ObjectMapper mapper;
    private final RestClient http = RestClient.create();

    public GoogleCalendarService(PlatformConfig config, TenantRepository tenantRepository,
                                 com.lms.platform.service.CentralTenantConfigReader centralTenantConfigReader, TenantSecretCipher cipher,
                                 ObjectMapper mapper) {
        this.config = config;
        this.tenantRepository = tenantRepository;
        this.centralTenantConfigReader = centralTenantConfigReader;
        this.cipher = cipher;
        this.mapper = mapper;
    }

    public String authorizationUrl() {
        TenantConnection tenant = TenantContext.current().orElseThrow(() -> new BusinessRuleException("Tenant context is required"));
        requireOAuthConfigured();
        String state = signedState(tenant.slug() + "|" + (Instant.now().getEpochSecond() + 600) + "|" + UUID.randomUUID());
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", config.getGoogleCalendar().getClientId())
                .queryParam("redirect_uri", config.getGoogleCalendar().getRedirectUri())
                .queryParam("response_type", "code").queryParam("scope", "openid email " + CALENDAR_SCOPE)
                .queryParam("access_type", "offline").queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "true").queryParam("state", state).build().encode().toUriString();
    }

    public String handleCallback(String code, String state) {
        String payload = verifyState(state);
        String[] fields = payload.split("\\|", 3);
        Tenant tenant = tenantRepository.findBySlug(fields[0]).orElseThrow(() -> new BusinessRuleException("Google connection tenant was not found"));
        if (tenant.getStatus() != com.lms.platform.entity.TenantStatus.ACTIVE) throw new BusinessRuleException("Tenant is not active");
        try {
            JsonNode tokens = http.post().uri("https://oauth2.googleapis.com/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form(Map.of("code", code, "client_id", config.getGoogleCalendar().getClientId(),
                            "client_secret", config.getGoogleCalendar().getClientSecret(),
                            "redirect_uri", config.getGoogleCalendar().getRedirectUri(), "grant_type", "authorization_code")))
                    .retrieve().body(JsonNode.class);
            String refreshToken = tokens.path("refresh_token").asText("");
            if (refreshToken.isBlank()) throw new BusinessRuleException("Google did not return offline access. Reconnect and grant access again.");
            String accessToken = tokens.path("access_token").asText();
            JsonNode profile = http.get().uri("https://www.googleapis.com/oauth2/v2/userinfo")
                    .header("Authorization", "Bearer " + accessToken).retrieve().body(JsonNode.class);
            centralTenantConfigReader.connectGoogle(tenant.getId(), cipher.encrypt(refreshToken), profile.path("email").asText(null));
            return tenant.getSlug();
        } finally {
        }
    }

    public MeetEvent createMeetEvent(TenantConfig tenantConfig, String title, String description,
                                     Instant start, Instant end) {
        if (tenantConfig == null || tenantConfig.getGoogleCalendarRefreshToken() == null)
            throw new BusinessRuleException("Connect an organization Google Calendar before scheduling Google Meet classes.");
        String refreshToken = cipher.decrypt(tenantConfig.getGoogleCalendarRefreshToken());
        JsonNode tokens = http.post().uri("https://oauth2.googleapis.com/token").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form(Map.of("client_id", config.getGoogleCalendar().getClientId(),
                        "client_secret", config.getGoogleCalendar().getClientSecret(),
                        "refresh_token", refreshToken, "grant_type", "refresh_token")))
                .retrieve().body(JsonNode.class);
        String accessToken = tokens.path("access_token").asText();
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of("summary", title, "description", description == null ? "" : description,
                "start", Map.of("dateTime", start.toString()), "end", Map.of("dateTime", end.toString()),
                "conferenceData", Map.of("createRequest", Map.of("requestId", requestId,
                        "conferenceSolutionKey", Map.of("type", "hangoutsMeet"))));
        JsonNode created = http.post().uri("https://www.googleapis.com/calendar/v3/calendars/primary/events?conferenceDataVersion=1")
                .header("Authorization", "Bearer " + accessToken).contentType(MediaType.APPLICATION_JSON)
                .body(event).retrieve().body(JsonNode.class);
        String eventId = created.path("id").asText();
        String meetUrl = created.path("hangoutLink").asText("");
        if (meetUrl.isBlank()) {
            for (int i = 0; i < 5 && meetUrl.isBlank(); i++) {
                try { Thread.sleep(700L); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }
                JsonNode refreshed = http.get().uri("https://www.googleapis.com/calendar/v3/calendars/primary/events/{id}?conferenceDataVersion=1", eventId)
                        .header("Authorization", "Bearer " + accessToken).retrieve().body(JsonNode.class);
                meetUrl = refreshed.path("hangoutLink").asText("");
            }
        }
        if (meetUrl.isBlank()) throw new BusinessRuleException("Google Calendar did not return a Meet link. Check that the connected account supports Google Meet.");
        return new MeetEvent(eventId, meetUrl);
    }

    private MultiValueMap<String, String> form(Map<String, String> values) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        values.forEach(form::add);
        return form;
    }

    private void requireOAuthConfigured() {
        if (config.getGoogleCalendar().getClientId() == null || config.getGoogleCalendar().getClientId().isBlank()
                || config.getGoogleCalendar().getClientSecret() == null || config.getGoogleCalendar().getClientSecret().isBlank()
                || config.getGoogleCalendar().getRedirectUri() == null || config.getGoogleCalendar().getRedirectUri().isBlank())
            throw new BusinessRuleException("Google OAuth is not configured. Set GOOGLE_OAUTH_CLIENT_ID, GOOGLE_OAUTH_CLIENT_SECRET, and GOOGLE_OAUTH_REDIRECT_URI.");
    }

    private String signedState(String payload) {
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + signature(encoded);
    }

    private String verifyState(String state) {
        try {
            String[] parts = state.split("\\.", 2);
            if (parts.length != 2 || !MessageDigest.isEqual(signature(parts[0]).getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8)))
                throw new IllegalArgumentException();
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            long expiry = Long.parseLong(payload.split("\\|", 3)[1]);
            if (expiry < Instant.now().getEpochSecond()) throw new IllegalArgumentException();
            return payload;
        } catch (Exception ex) { throw new BusinessRuleException("Google authorization state is invalid or expired. Please try again."); }
    }

    private String signature(String input) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(config.getGoogleCalendar().getClientSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    public record MeetEvent(String eventId, String meetingUrl) {}
}
