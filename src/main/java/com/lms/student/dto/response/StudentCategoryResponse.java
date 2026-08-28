package com.lms.student.dto.response;

import lombok.Data;

import java.util.UUID;

@Data
public class StudentCategoryResponse {

    private UUID id;
    private String name;
    private String description;
    private int sortOrder;

    /** How many learners hold this category — deleting one in use is refused. */
    private long learnerCount;
}
