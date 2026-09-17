package com.lms.notes.repository;

import com.lms.notes.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

    Page<Note> findByStudentIdOrderByUpdatedAtDesc(UUID studentId, Pageable pageable);

    List<Note> findByStudentIdAndCourseIdOrderByUpdatedAtDesc(UUID studentId, UUID courseId);

    Optional<Note> findByStudentIdAndCourseIdAndLessonId(UUID studentId, UUID courseId, UUID lessonId);

    Optional<Note> findByIdAndStudentId(UUID id, UUID studentId);

    long countByStudentId(UUID studentId);
}
