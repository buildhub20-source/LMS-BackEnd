package com.lms.enrollment.repository;

import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID>, JpaSpecificationExecutor<Enrollment> {

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    // Queries for Student
    Page<Enrollment> findByStudentId(UUID studentId, Pageable pageable);
    Optional<Enrollment> findByIdAndStudentId(UUID id, UUID studentId);

    // Queries for Instructor - strictly enforcing ownership
    @Query("SELECT e FROM Enrollment e WHERE e.course.instructorId = :instructorId")
    Page<Enrollment> findByCourseInstructorId(@Param("instructorId") UUID instructorId, Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.id = :id AND e.course.instructorId = :instructorId")
    Optional<Enrollment> findByIdAndCourseInstructorId(@Param("id") UUID id, @Param("instructorId") UUID instructorId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.instructorId = :instructorId AND e.status = :status")
    long countByCourseInstructorIdAndStatus(@Param("instructorId") UUID instructorId, @Param("status") EnrollmentStatus status);
}
