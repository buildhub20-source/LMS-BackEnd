package com.lms.live.repository;

import com.lms.live.entity.LiveSession;
import com.lms.live.entity.LiveSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveSessionRepository extends JpaRepository<LiveSession, UUID> {

    List<LiveSession> findByCourseIdOrderByScheduledStartDesc(UUID courseId);

    List<LiveSession> findByCourseIdAndStatus(UUID courseId, LiveSessionStatus status);

    List<LiveSession> findByTenantIdOrderByScheduledStartDesc(UUID tenantId);

    List<LiveSession> findByInstructorIdOrderByScheduledStartDesc(UUID instructorId);

    @Query("SELECT s FROM LiveSession s WHERE s.course.id IN :courseIds ORDER BY s.scheduledStart DESC")
    List<LiveSession> findByCourseIdInOrderByScheduledStartDesc(@Param("courseIds") List<UUID> courseIds);

    Optional<LiveSession> findByRoomName(String roomName);

    /** Scheduled or live classes whose scheduled end is before {@code before}; candidates for expiry. */
    @Query("SELECT s FROM LiveSession s WHERE s.tenantId = :tenantId AND s.status IN ('SCHEDULED', 'LIVE') "
            + "AND s.scheduledEnd < :before")
    List<LiveSession> findOpenEndedBefore(@Param("tenantId") UUID tenantId, @Param("before") java.time.Instant before);

    /**
     * Classes still to come whose start falls at or before {@code dueBefore} —
     * the candidates for a reminder mail.
     *
     * <p>{@code notBefore} is the caller's floor: it keeps a class already
     * under way out of the results, and lets the day-before reminder exclude
     * classes that are nearly starting. CANCELLED and MISSED are excluded by
     * the status filter.
     *
     * <p>The course is fetch-joined because the reminder sweep runs outside a
     * transaction: without it, reading {@code session.getCourse()} to name the
     * course in the mail would fail on the detached entity.
     */
    @Query("SELECT s FROM LiveSession s JOIN FETCH s.course WHERE s.status = 'SCHEDULED' "
            + "AND s.scheduledStart > :notBefore AND s.scheduledStart <= :dueBefore "
            + "ORDER BY s.scheduledStart")
    List<LiveSession> findScheduledStartingBetween(@Param("notBefore") java.time.Instant notBefore,
                                                   @Param("dueBefore") java.time.Instant dueBefore);

    @Query("SELECT COUNT(s) FROM LiveSession s WHERE s.tenantId = :tenantId AND s.status = 'LIVE'")
    long countActiveSessionsByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(s) FROM LiveSession s WHERE s.instructor.id = :instructorId AND s.status = 'LIVE'")
    long countActiveSessionsByInstructorId(@Param("instructorId") UUID instructorId);
}
