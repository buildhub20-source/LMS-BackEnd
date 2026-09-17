package com.lms.notes.repository;

import com.lms.notes.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {

    Page<Bookmark> findByStudentIdOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

    List<Bookmark> findByStudentIdAndCourseIdOrderByCreatedAtDesc(UUID studentId, UUID courseId);

    Optional<Bookmark> findByStudentIdAndCourseIdAndLessonId(UUID studentId, UUID courseId, UUID lessonId);

    Optional<Bookmark> findByIdAndStudentId(UUID id, UUID studentId);

    boolean existsByStudentIdAndCourseIdAndLessonId(UUID studentId, UUID courseId, UUID lessonId);

    long countByStudentId(UUID studentId);
}
