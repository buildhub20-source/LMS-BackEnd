package com.lms.notes.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.common.response.PageResponse;
import com.lms.notes.dto.NoteRequest;
import com.lms.notes.dto.NoteResponse;
import com.lms.notes.service.NoteService;
import com.lms.security.authentication.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.STUDENT_NOTES)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
public class NoteController {

    private final NoteService noteService;

    /** Get all notes for the current student, paginated. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NoteResponse>>> getAllNotes(Pageable pageable) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(noteService.getStudentNotes(studentId, pageable)));
    }

    /** Get notes for a specific course. */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> getNotesByCourse(@PathVariable UUID courseId) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(noteService.getNotesByCourse(studentId, courseId)));
    }

    /** Get the note for a specific lesson (may be null). */
    @GetMapping("/course/{courseId}/lesson/{lessonId}")
    public ResponseEntity<ApiResponse<NoteResponse>> getNoteForLesson(
            @PathVariable UUID courseId, @PathVariable UUID lessonId) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(noteService.getNoteForLesson(studentId, courseId, lessonId)));
    }

    /** Create or update a note (upsert by student + course + lesson). */
    @PostMapping
    public ResponseEntity<ApiResponse<NoteResponse>> saveNote(@Valid @RequestBody NoteRequest request) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(noteService.saveNote(studentId, request)));
    }

    /** Delete a note by ID. */
    @DeleteMapping("/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(@PathVariable UUID noteId) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        noteService.deleteNote(studentId, noteId);
        return ResponseEntity.ok(ApiResponse.message("Note deleted"));
    }
}
