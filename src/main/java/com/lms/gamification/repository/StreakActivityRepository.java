package com.lms.gamification.repository;

import com.lms.gamification.entity.StreakActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface StreakActivityRepository extends JpaRepository<StreakActivity, UUID> {
    boolean existsByStudentIdAndActivityDate(UUID studentId, LocalDate activityDate);
}
