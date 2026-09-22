package com.lms.live.repository;

import com.lms.live.entity.LiveSessionAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveSessionAttendanceRepository extends JpaRepository<LiveSessionAttendance, UUID> {

    List<LiveSessionAttendance> findBySessionIdOrderByJoinedAtAsc(UUID sessionId);

    Optional<LiveSessionAttendance> findFirstBySessionIdAndUserIdAndLeftAtIsNullOrderByJoinedAtDesc(UUID sessionId, UUID userId);

    @Query("SELECT COUNT(a) FROM LiveSessionAttendance a WHERE a.session.id = :sessionId AND a.leftAt IS NULL")
    long countActiveParticipants(@Param("sessionId") UUID sessionId);

    @Query("SELECT COUNT(DISTINCT a.user.id) FROM LiveSessionAttendance a WHERE a.session.id = :sessionId")
    long countTotalUniqueParticipants(@Param("sessionId") UUID sessionId);
}
