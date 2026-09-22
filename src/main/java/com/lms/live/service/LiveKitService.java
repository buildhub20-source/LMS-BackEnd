package com.lms.live.service;

import com.lms.config.LiveKitConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveKitService {

    private final LiveKitConfig liveKitConfig;

    /**
     * Builds deterministic room name: tenant-{tenantId}-session-{sessionId}
     */
    public String buildRoomName(UUID tenantId, UUID sessionId) {
        String tId = tenantId != null ? tenantId.toString() : "default";
        return "tenant-" + tId + "-session-" + sessionId.toString();
    }

    /**
     * Generates a short-lived LiveKit participant token signed with HS256.
     */
    public String generateParticipantToken(
            String roomName,
            String identity,
            String participantName,
            boolean isInstructor,
            boolean screenShareEnabled
    ) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds((long) liveKitConfig.getTokenTtlMinutes() * 60L);

        Map<String, Object> videoGrants = new HashMap<>();
        videoGrants.put("room", roomName);
        videoGrants.put("roomJoin", true);
        videoGrants.put("canSubscribe", true);
        videoGrants.put("canPublishData", true);

        if (isInstructor) {
            videoGrants.put("canPublish", true);
            videoGrants.put("canPublishSources", new String[]{"camera", "microphone", "screen_share"});
        } else {
            // Student permissions
            videoGrants.put("canPublish", false);
            if (screenShareEnabled) {
                videoGrants.put("canPublishSources", new String[]{"screen_share"});
            }
        }

        SecretKey key = getSigningKey(liveKitConfig.getApiSecret());

        return Jwts.builder()
                .issuer(liveKitConfig.getApiKey())
                .subject(identity)
                .claim("name", participantName)
                .claim("video", videoGrants)
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public String getServerUrl() {
        return liveKitConfig.getUrl();
    }

    /**
     * Validates incoming LiveKit webhook signature or auth token.
     */
    public boolean verifyWebhook(String authHeader, String rawPayload) {
        if (authHeader == null || authHeader.isBlank()) {
            return false;
        }
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        try {
            SecretKey key = getSigningKey(liveKitConfig.getApiSecret());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(liveKitConfig.getApiKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String encodedPayloadHash = claims.get("sha256", String.class);
            if (encodedPayloadHash == null || encodedPayloadHash.isBlank()) {
                return false;
            }
            byte[] expectedHash = Base64.getDecoder().decode(encodedPayloadHash);
            byte[] actualHash = MessageDigest.getInstance("SHA-256")
                    .digest(rawPayload.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            log.warn("Failed to verify LiveKit webhook signature: {}", e.getMessage());
            return false;
        }
    }

    private SecretKey getSigningKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            try {
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                keyBytes = sha.digest(keyBytes);
            } catch (NoSuchAlgorithmException ignored) {
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
