package com.lms.notes.service;

import com.lms.notes.dto.BookmarkRequest;
import com.lms.notes.entity.Bookmark;
import com.lms.notes.repository.BookmarkRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookmarkServiceTest {

    private final BookmarkRepository repository = mock(BookmarkRepository.class);
    private final BookmarkService service = new BookmarkService(repository);
    private final UUID student = UUID.randomUUID();
    private final UUID course = UUID.randomUUID();

    @Test
    void bookmarksAWholeCourseWhenNoLessonIsGiven() {
        when(repository.findFirstByStudentIdAndCourseIdAndLessonIdIsNull(student, course)).thenReturn(Optional.empty());

        boolean now = service.toggleBookmark(student, BookmarkRequest.builder().courseId(course).label("Java").build());

        assertThat(now).isTrue();
        ArgumentCaptor<Bookmark> saved = ArgumentCaptor.forClass(Bookmark.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getCourseId()).isEqualTo(course);
        assertThat(saved.getValue().getLessonId()).isNull();
        verify(repository, never()).findByStudentIdAndCourseIdAndLessonId(any(), any(), any());
    }

    @Test
    void removesTheCourseBookmarkOnSecondToggle() {
        Bookmark existing = Bookmark.builder().studentId(student).courseId(course).build();
        when(repository.findFirstByStudentIdAndCourseIdAndLessonIdIsNull(student, course)).thenReturn(Optional.of(existing));

        boolean now = service.toggleBookmark(student, BookmarkRequest.builder().courseId(course).build());

        assertThat(now).isFalse();
        verify(repository).delete(existing);
        verify(repository, never()).save(any());
    }

    @Test
    void lessonBookmarksAreSeparateFromTheCourseBookmark() {
        UUID lesson = UUID.randomUUID();
        when(repository.findByStudentIdAndCourseIdAndLessonId(student, course, lesson)).thenReturn(Optional.empty());

        assertThat(service.isBookmarked(student, course, lesson)).isFalse();
        verify(repository, never()).findFirstByStudentIdAndCourseIdAndLessonIdIsNull(any(), any());
    }
}
