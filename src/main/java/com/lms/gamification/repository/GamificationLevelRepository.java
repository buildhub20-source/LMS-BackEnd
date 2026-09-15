package com.lms.gamification.repository;

import com.lms.gamification.entity.GamificationLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GamificationLevelRepository extends JpaRepository<GamificationLevel, UUID> {
    Optional<GamificationLevel> findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(int points);
    List<GamificationLevel> findAllByOrderByLevelNumberAsc();
}
