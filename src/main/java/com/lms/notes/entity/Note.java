package com.lms.notes.entity;

import com.lms.common.audit.Timestamped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

/**
 * A student's note attached to a specific course and optionally to a lesson.
 * Notes support markdown-formatted content.
 */
@Entity
@Table(name = "student_notes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_note_student_course_lesson",
                columnNames = {"student_id", "course_id", "lesson_id"})
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Note extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_id")
    private UUID courseId;

    /** Optional — null means the note is a general course-level note. */
    @Column(name = "lesson_id")
    private UUID lessonId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Note n)) return false;
        return id != null && id.equals(n.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
