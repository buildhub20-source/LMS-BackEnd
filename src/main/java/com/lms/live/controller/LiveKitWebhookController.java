package com.lms.live.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.common.constants.ApiPaths;
import com.lms.live.entity.LiveSession;
import com.lms.live.entity.LiveSessionStatus;
import com.lms.live.repository.LiveSessionAttendanceRepository;
import com.lms.live.repository.LiveSessionRepository;
import com.lms.live.service.LiveAttendanceService;
import com.lms.live.service.LiveKitService;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(ApiPaths.INTEGRATIONS + "/livekit")
@RequiredArgsConstructor
public class LiveKitWebhookController {

    private final LiveKitService liveKitService;
    private final LiveSessionRepository sessionRepository;
    private final LiveSessionAttendanceRepository attendanceRepository;
    private final LiveAttendanceService attendanceService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody String rawPayload
    ) {
        // LiveKit signs every webhook and includes a SHA-256 hash of the raw body.
        // Reject missing headers as well as tokens whose signature or body hash is invalid.
        if (!liveKitService.verifyWebhook(authHeader, rawPayload)) {
            log.warn("Unauthorized LiveKit webhook attempt rejected.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String event = root.path("event").asText("");
            String roomName = root.path("room").path("name").asText("");

            if (roomName.isBlank()) {
                return ResponseEntity.ok("Ignored: no roomName");
            }

            Optional<LiveSession> sessionOpt = sessionRepository.findByRoomName(roomName);
            if (sessionOpt.isEmpty()) {
                log.debug("LiveKit webhook received for untracked room: {}", roomName);
                return ResponseEntity.ok("Ignored: session not found");
            }

            LiveSession session = sessionOpt.get();

            switch (event) {
                case "participant_joined" -> {
                    String identity = root.path("participant").path("identity").asText("");
                    resolveUser(identity).ifPresent(user ->
                            attendanceService.recordParticipantJoined(session, user));
                }
                case "participant_left" -> {
                    String identity = root.path("participant").path("identity").asText("");
                    resolveUser(identity).ifPresent(user ->
                            attendanceService.recordParticipantLeft(session, user));
                }
                case "room_started" -> {
                    if (session.getStatus() == LiveSessionStatus.SCHEDULED) {
                        session.setStatus(LiveSessionStatus.LIVE);
                        session.setActualStart(Instant.now());
                        sessionRepository.save(session);
                    }
                }
                case "room_finished" -> {
                    if (session.getStatus() == LiveSessionStatus.LIVE) {
                        session.setStatus(LiveSessionStatus.ENDED);
                        session.setActualEnd(Instant.now());
                        sessionRepository.save(session);

                        attendanceRepository.findBySessionIdOrderByJoinedAtAsc(session.getId()).forEach(a -> {
                            if (a.getLeftAt() == null) {
                                attendanceService.recordParticipantLeft(session, a.getUser());
                            }
                        });
                    }
                }
                case "egress_ended" -> {
                    String downloadUrl = root.path("egressInfo").path("fileResults").path(0).path("downloadUrl").asText("");
                    if (!downloadUrl.isBlank()) {
                        session.setRecordingUrl(downloadUrl);
                        sessionRepository.save(session);
                    }
                }
                default -> log.debug("Unhandled LiveKit event: {}", event);
            }

            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            log.error("Error processing LiveKit webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }

    private Optional<User> resolveUser(String identity) {
        if (identity == null || identity.isBlank()) return Optional.empty();
        try {
            UUID userId = UUID.fromString(identity);
            return userRepository.findById(userId);
        } catch (IllegalArgumentException e) {
            return userRepository.findByEmailIgnoreCase(identity);
        }
    }
}
