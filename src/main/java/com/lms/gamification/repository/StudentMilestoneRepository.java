package com.lms.gamification.repository;

import com.lms.gamification.entity.StudentMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentMilestoneRepository extends JpaRepository<StudentMilestone, UUID> {
    List<StudentMilestone> findByStudentId(UUID studentId);
    boolean existsByStudentIdAndMilestoneId(UUID studentId, UUID milestoneId);
}
