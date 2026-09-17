package com.lms.notes.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class NoteRequest {

    /** Optional — null for a general study note. */
    private UUID courseId;

    /** Optional — null for a general course-level note. */
    private UUID lessonId;

    private String title;

    private String content;
}
