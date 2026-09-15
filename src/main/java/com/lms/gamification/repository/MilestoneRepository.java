package com.lms.gamification.repository;

import com.lms.gamification.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {
    List<Milestone> findByActiveTrueOrderBySortOrderAsc();
}
