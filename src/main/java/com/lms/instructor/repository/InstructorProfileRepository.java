package com.lms.instructor.repository;

import com.lms.instructor.entity.InstructorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface InstructorProfileRepository
        extends JpaRepository<InstructorProfile, UUID>, JpaSpecificationExecutor<InstructorProfile> {

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);

    Optional<InstructorProfile> findByUserId(UUID userId);
}
