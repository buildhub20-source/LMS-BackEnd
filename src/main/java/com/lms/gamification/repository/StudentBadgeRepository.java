package com.lms.gamification.repository;

import com.lms.gamification.entity.StudentBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentBadgeRepository extends JpaRepository<StudentBadge, UUID> {
    List<StudentBadge> findByStudentId(UUID studentId);
    boolean existsByStudentIdAndBadgeId(UUID studentId, UUID badgeId);
    long countByStudentId(UUID studentId);
}
