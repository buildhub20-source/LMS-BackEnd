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
public class BookmarkRequest {

    @NotNull
    private UUID courseId;

    @NotNull
    private UUID lessonId;

    private String label;
}
