package com.lms.gamification.repository;

import com.lms.gamification.entity.PointRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PointRuleRepository extends JpaRepository<PointRule, UUID> {
    Optional<PointRule> findByEventTypeAndActiveTrue(String eventType);
    List<PointRule> findByActiveTrue();
}
