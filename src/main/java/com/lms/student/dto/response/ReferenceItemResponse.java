package com.lms.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A dropdown option: identifier plus the label the form renders. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceItemResponse {

    private UUID id;
    private String label;
}
