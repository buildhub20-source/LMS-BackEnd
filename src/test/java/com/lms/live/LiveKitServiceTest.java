package com.lms.live;

import com.lms.config.LiveKitConfig;
import com.lms.live.service.LiveKitService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LiveKitServiceTest {

    private LiveKitService liveKitService;
    private LiveKitConfig config;

    @BeforeEach
    void setUp() {
        config = new LiveKitConfig();
        config.setUrl("wss://test.livekit.cloud");
        config.setApiKey("test-api-key");
        config.setApiSecret("test-secret-key-that-is-at-least-32-bytes-long!!");
        config.setTokenTtlMinutes(60);
        liveKitService = new LiveKitService(config);
    }

    @Test
    void testBuildRoomName() {
        UUID tenantId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String roomName = liveKitService.buildRoomName(tenantId, sessionId);

        assertEquals("tenant-" + tenantId + "-session-" + sessionId, roomName);
    }

    @Test
    void testGenerateParticipantTokenForInstructor() {
        String room = "tenant-1-session-100";
        String identity = "user-123";
        String name = "Prof. Xavier";

        String token = liveKitService.generateParticipantToken(room, identity, name, true, true);
        assertNotNull(token);
        assertFalse(token.isBlank());

        // Parse and inspect claims
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(config.getApiSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("test-api-key", claims.getIssuer());
        assertEquals("user-123", claims.getSubject());
        assertEquals("Prof. Xavier", claims.get("name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> video = (Map<String, Object>) claims.get("video");
        assertNotNull(video);
        assertEquals(room, video.get("room"));
        assertEquals(Boolean.TRUE, video.get("roomJoin"));
        assertEquals(Boolean.TRUE, video.get("canPublish"));
        assertEquals(Boolean.TRUE, video.get("canSubscribe"));
    }

    @Test
    void testGenerateParticipantTokenForStudent() {
        String room = "tenant-1-session-100";
        String identity = "student-456";
        String name = "Alice Student";

        String token = liveKitService.generateParticipantToken(room, identity, name, false, false);
        assertNotNull(token);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(config.getApiSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        @SuppressWarnings("unchecked")
        Map<String, Object> video = (Map<String, Object>) claims.get("video");
        assertNotNull(video);
        assertEquals(Boolean.FALSE, video.get("canPublish"));
        assertEquals(Boolean.TRUE, video.get("canSubscribe"));
    }

    @Test
    void verifiesSignedWebhookAndPayloadHash() throws Exception {
        String payload = "{\"event\":\"room_started\"}";
        String payloadHash = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        Instant now = Instant.now();
        String authToken = Jwts.builder()
                .issuer(config.getApiKey())
                .claim("sha256", payloadHash)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(config.getApiSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertTrue(liveKitService.verifyWebhook(authToken, payload));
        assertFalse(liveKitService.verifyWebhook(authToken, payload + " "));
        assertFalse(liveKitService.verifyWebhook(null, payload));
    }
}
