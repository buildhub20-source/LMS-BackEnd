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
 * A student's bookmark on a specific lesson within a course.
 * Acts as a "favorite" marker for quick access later.
 */
@Entity
@Table(name = "student_bookmarks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bookmark_student_course_lesson",
                columnNames = {"student_id", "course_id", "lesson_id"})
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Bookmark extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    /** Optional user-facing label, e.g. "Important formula" */
    @Column(name = "label", length = 255)
    private String label;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Bookmark b)) return false;
        return id != null && id.equals(b.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
