package com.lms.notes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class NoteResponse {

    private UUID id;
    private UUID courseId;
    private UUID lessonId;
    private String title;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
