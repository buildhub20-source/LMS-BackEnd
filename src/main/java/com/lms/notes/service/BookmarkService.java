package com.lms.notes.service;

import com.lms.common.response.PageResponse;
import com.lms.notes.dto.BookmarkRequest;
import com.lms.notes.dto.BookmarkResponse;
import com.lms.notes.entity.Bookmark;
import com.lms.notes.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;

    @Transactional(readOnly = true)
    public PageResponse<BookmarkResponse> getStudentBookmarks(UUID studentId, Pageable pageable) {
        Page<Bookmark> page = bookmarkRepository.findByStudentIdOrderByCreatedAtDesc(studentId, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<BookmarkResponse> getBookmarksByCourse(UUID studentId, UUID courseId) {
        return bookmarkRepository.findByStudentIdAndCourseIdOrderByCreatedAtDesc(studentId, courseId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(UUID studentId, UUID courseId, UUID lessonId) {
        return bookmarkRepository.existsByStudentIdAndCourseIdAndLessonId(studentId, courseId, lessonId);
    }

    /**
     * Toggle bookmark — if already bookmarked, removes it; otherwise creates it.
     * Returns true if the bookmark now exists, false if it was removed.
     */
    @Transactional
    public boolean toggleBookmark(UUID studentId, BookmarkRequest request) {
        var existing = bookmarkRepository.findByStudentIdAndCourseIdAndLessonId(
                studentId, request.getCourseId(), request.getLessonId());

        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            return false;
        }

        bookmarkRepository.save(Bookmark.builder()
                .studentId(studentId)
                .courseId(request.getCourseId())
                .lessonId(request.getLessonId())
                .label(request.getLabel())
                .build());
        return true;
    }

    @Transactional
    public void deleteBookmark(UUID studentId, UUID bookmarkId) {
        bookmarkRepository.findByIdAndStudentId(bookmarkId, studentId)
                .ifPresent(bookmarkRepository::delete);
    }

    private BookmarkResponse toResponse(Bookmark bookmark) {
        return BookmarkResponse.builder()
                .id(bookmark.getId())
                .courseId(bookmark.getCourseId())
                .lessonId(bookmark.getLessonId())
                .label(bookmark.getLabel())
                .createdAt(bookmark.getCreatedAt())
                .build();
    }
}
