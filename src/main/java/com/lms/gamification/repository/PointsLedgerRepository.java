package com.lms.gamification.repository;

import com.lms.gamification.entity.PointsLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface PointsLedgerRepository extends JpaRepository<PointsLedger, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Page<PointsLedger> findByStudentIdOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointsLedger p WHERE p.studentId = :studentId")
    int sumPointsByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointsLedger p WHERE p.studentId = :studentId AND p.createdAt >= :since")
    int sumPointsByStudentIdSince(@Param("studentId") UUID studentId, @Param("since") Instant since);

    long countByStudentIdAndEventType(UUID studentId, String eventType);
}
