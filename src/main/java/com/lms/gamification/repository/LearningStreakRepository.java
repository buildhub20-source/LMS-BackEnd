package com.lms.gamification.repository;

import com.lms.gamification.entity.LearningStreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LearningStreakRepository extends JpaRepository<LearningStreak, UUID> {
    Optional<LearningStreak> findByStudentId(UUID studentId);
}
