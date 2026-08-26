package com.lms.student.repository;

import com.lms.student.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface StudentProfileRepository
        extends JpaRepository<StudentProfile, UUID>, JpaSpecificationExecutor<StudentProfile> {

    boolean existsByRegistrationNoIgnoreCase(String registrationNo);

    /** Guards category deletion, and reports usage on the category list. */
    long countByCategoryId(UUID categoryId);

    Optional<StudentProfile> findByUserId(UUID userId);

    /**
     * Fetches the profile with its enrolments in one query. Every response maps
     * enrolments, so the lazy collection would otherwise be an N+1.
     */
    @Query("select distinct p from StudentProfile p left join fetch p.enrolments where p.id = :id")
    Optional<StudentProfile> findByIdWithEnrolments(UUID id);
}
