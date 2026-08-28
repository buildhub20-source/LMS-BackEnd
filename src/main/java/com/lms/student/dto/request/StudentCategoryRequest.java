package com.lms.student.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload for creating or renaming an admission category. */
@Data
public class StudentCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 50, message = "Category name must not exceed 50 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @PositiveOrZero(message = "Sort order cannot be negative")
    private Integer sortOrder;
}
