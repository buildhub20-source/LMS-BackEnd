package com.lms.live.service;

import com.lms.live.dto.LiveAttendanceResponse;
import com.lms.live.entity.LiveSession;
import com.lms.live.entity.LiveSessionAttendance;
import com.lms.live.repository.LiveSessionAttendanceRepository;
import com.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveAttendanceService {

    private final LiveSessionAttendanceRepository attendanceRepository;
    private final LiveUsageService liveUsageService;

    @Transactional
    public LiveSessionAttendance recordParticipantJoined(LiveSession session, User user) {
        return attendanceRepository.findFirstBySessionIdAndUserIdAndLeftAtIsNullOrderByJoinedAtDesc(session.getId(), user.getId())
                .orElseGet(() -> {
                    LiveSessionAttendance attendance = LiveSessionAttendance.builder()
                            .session(session)
                            .user(user)
                            .joinedAt(Instant.now())
                            .durationSeconds(0L)
                            .build();
                    LiveSessionAttendance saved = attendanceRepository.save(attendance);

                    long activeCount = attendanceRepository.countActiveParticipants(session.getId());
                    liveUsageService.recordPeakParticipants(session.getTenantId(), (int) activeCount);

                    log.info("Participant {} joined live session {}", user.getEmail(), session.getId());
                    return saved;
                });
    }

    @Transactional
    public void recordParticipantLeft(LiveSession session, User user) {
        attendanceRepository.findFirstBySessionIdAndUserIdAndLeftAtIsNullOrderByJoinedAtDesc(session.getId(), user.getId())
                .ifPresent(record -> {
                    Instant now = Instant.now();
                    record.setLeftAt(now);
                    long seconds = Math.max(0, Duration.between(record.getJoinedAt(), now).getSeconds());
                    record.setDurationSeconds(seconds);
                    attendanceRepository.save(record);

                    liveUsageService.recordParticipantDuration(session.getTenantId(), seconds);
                    log.info("Participant {} left live session {}. Attended {} seconds",
                            user.getEmail(), session.getId(), seconds);
                });
    }

    /**
     * Closes everyone still marked as present when a class is ended
     * automatically. Time is counted only up to {@code endedAt}, never up to
     * now, so a forgotten class does not bill days of participant minutes.
     */
    @Transactional
    public void closeOpenAttendance(LiveSession session, Instant endedAt) {
        attendanceRepository.findBySessionIdOrderByJoinedAtAsc(session.getId()).stream()
                .filter(record -> record.getLeftAt() == null)
                .forEach(record -> {
                    Instant leftAt = record.getJoinedAt().isAfter(endedAt) ? record.getJoinedAt() : endedAt;
                    long seconds = Math.max(0, Duration.between(record.getJoinedAt(), leftAt).getSeconds());
                    record.setLeftAt(leftAt);
                    record.setDurationSeconds(seconds);
                    attendanceRepository.save(record);
                    liveUsageService.recordParticipantDuration(session.getTenantId(), seconds);
                });
    }

    @Transactional(readOnly = true)
    public List<LiveAttendanceResponse> getSessionAttendance(UUID sessionId) {
        return attendanceRepository.findBySessionIdOrderByJoinedAtAsc(sessionId)
                .stream()
                .map(LiveAttendanceResponse::from)
                .toList();
    }
}
