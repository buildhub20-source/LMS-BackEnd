package com.lms.notes.service;

import com.lms.common.response.PageResponse;
import com.lms.notes.dto.NoteRequest;
import com.lms.notes.dto.NoteResponse;
import com.lms.notes.entity.Note;
import com.lms.notes.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    @Transactional(readOnly = true)
    public PageResponse<NoteResponse> getStudentNotes(UUID studentId, Pageable pageable) {
        Page<Note> page = noteRepository.findByStudentIdOrderByUpdatedAtDesc(studentId, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> getNotesByCourse(UUID studentId, UUID courseId) {
        return noteRepository.findByStudentIdAndCourseIdOrderByUpdatedAtDesc(studentId, courseId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public NoteResponse getNoteForLesson(UUID studentId, UUID courseId, UUID lessonId) {
        return noteRepository.findByStudentIdAndCourseIdAndLessonId(studentId, courseId, lessonId)
                .map(this::toResponse).orElse(null);
    }

    /**
     * Creates or updates a note. If a note already exists for the same
     * student + course + lesson, it is updated (upsert semantics).
     */
    @Transactional
    public NoteResponse saveNote(UUID studentId, NoteRequest request) {
        Note note = null;
        if (request.getCourseId() != null) {
            note = noteRepository.findByStudentIdAndCourseIdAndLessonId(
                            studentId, request.getCourseId(), request.getLessonId())
                    .orElse(null);
        }
        if (note == null) {
            note = Note.builder()
                    .studentId(studentId)
                    .courseId(request.getCourseId())
                    .lessonId(request.getLessonId())
                    .build();
        }

        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        return toResponse(noteRepository.save(note));
    }

    @Transactional
    public void deleteNote(UUID studentId, UUID noteId) {
        noteRepository.findByIdAndStudentId(noteId, studentId)
                .ifPresent(noteRepository::delete);
    }

    private NoteResponse toResponse(Note note) {
        return NoteResponse.builder()
                .id(note.getId())
                .courseId(note.getCourseId())
                .lessonId(note.getLessonId())
                .title(note.getTitle())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
