package com.lms.course.service;

import com.lms.course.entity.LearningTime;
import com.lms.course.repository.LearningTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Records study time from lesson-player heartbeats.
 *
 * <p>The client says how long it has been visible since its last heartbeat, but
 * the server never credits more than the wall-clock time since the previous
 * heartbeat, nor more than {@link #MAX_PER_HEARTBEAT} at once, so a replayed or
 * forged request cannot inflate the total.
 */
@Service
@RequiredArgsConstructor
public class LearningTimeService {

    static final int MAX_PER_HEARTBEAT = 90;
    static final int MAX_PER_DAY = 16 * 60 * 60;

    private final LearningTimeRepository repository;

    /** Returns the seconds actually credited. */
    @Transactional
    public int record(UUID studentId, UUID courseId, int requestedSeconds, Instant now) {
        int requested = Math.max(0, Math.min(requestedSeconds, MAX_PER_HEARTBEAT));
        LocalDate day = LocalDate.ofInstant(now, ZoneOffset.UTC);
        LearningTime row = repository.findByStudentIdAndCourseIdAndDay(studentId, courseId, day)
                .orElseGet(() -> LearningTime.builder().studentId(studentId).courseId(courseId).day(day).build());

        int credit = requested;
        if (row.getLastHeartbeatAt() != null) {
            long sinceLast = Duration.between(row.getLastHeartbeatAt(), now).toSeconds();
            credit = (int) Math.min(credit, Math.max(0, sinceLast));
        }
        credit = Math.min(credit, MAX_PER_DAY - row.getSeconds());

        row.setSeconds(row.getSeconds() + Math.max(0, credit));
        row.setLastHeartbeatAt(now);
        repository.save(row);
        return Math.max(0, credit);
    }

    /** Seconds per UTC day for a student over [from, to]. */
    @Transactional(readOnly = true)
    public Map<LocalDate, Long> secondsByDay(UUID studentId, LocalDate from, LocalDate to) {
        Map<LocalDate, Long> byDay = new HashMap<>();
        for (Object[] row : repository.sumSecondsByDay(studentId, from, to)) {
            byDay.put((LocalDate) row[0], ((Number) row[1]).longValue());
        }
        return byDay;
    }
}
