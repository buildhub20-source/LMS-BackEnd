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

    @Query("SELECT COUNT(s) FROM LiveSession s WHERE s.tenantId = :tenantId AND s.status = 'LIVE'")
    long countActiveSessionsByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(s) FROM LiveSession s WHERE s.instructor.id = :instructorId AND s.status = 'LIVE'")
    long countActiveSessionsByInstructorId(@Param("instructorId") UUID instructorId);
}
