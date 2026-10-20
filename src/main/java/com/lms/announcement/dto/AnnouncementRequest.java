package com.lms.announcement.dto;

import com.lms.announcement.entity.AnnouncementType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AnnouncementRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 5000)
    private String message;

    /** Defaults to INFO when omitted. */
    private AnnouncementType type;

    @Future
    private Instant expiresAt;
}
