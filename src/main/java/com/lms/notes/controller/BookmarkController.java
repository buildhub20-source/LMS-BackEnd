package com.lms.notes.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.common.response.PageResponse;
import com.lms.notes.dto.BookmarkRequest;
import com.lms.notes.dto.BookmarkResponse;
import com.lms.notes.service.BookmarkService;
import com.lms.security.authentication.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.STUDENT_BOOKMARKS)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    /** Get all bookmarks for the current student, paginated. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookmarkResponse>>> getAllBookmarks(Pageable pageable) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(bookmarkService.getStudentBookmarks(studentId, pageable)));
    }

    /** Get bookmarks for a specific course. */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<BookmarkResponse>>> getBookmarksByCourse(@PathVariable UUID courseId) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(bookmarkService.getBookmarksByCourse(studentId, courseId)));
    }

    /** Check if a specific lesson is bookmarked. */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> isBookmarked(
            @RequestParam UUID courseId, @RequestParam UUID lessonId) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        boolean bookmarked = bookmarkService.isBookmarked(studentId, courseId, lessonId);
        return ResponseEntity.ok(ApiResponse.of(Map.of("bookmarked", bookmarked)));
    }

    /** Toggle bookmark on/off for a lesson. Returns { bookmarked: true/false }. */
    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleBookmark(
            @Valid @RequestBody BookmarkRequest request) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        boolean nowBookmarked = bookmarkService.toggleBookmark(studentId, request);
        return ResponseEntity.ok(ApiResponse.of(Map.of("bookmarked", nowBookmarked)));
    }

    /** Delete a bookmark by ID. */
    @DeleteMapping("/{bookmarkId}")
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(@PathVariable UUID bookmarkId) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        bookmarkService.deleteBookmark(studentId, bookmarkId);
        return ResponseEntity.ok(ApiResponse.message("Bookmark removed"));
    }
}
