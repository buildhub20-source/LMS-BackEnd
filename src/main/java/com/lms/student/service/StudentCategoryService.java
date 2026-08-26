package com.lms.student.service;

import com.lms.student.dto.request.StudentCategoryRequest;
import com.lms.student.dto.response.StudentCategoryResponse;

import java.util.List;
import java.util.UUID;

/**
 * Admission categories.
 *
 * <p>Editable reference data rather than an enum: a centre renames these, and
 * the intake form has to pick up the change without a deployment.
 */
public interface StudentCategoryService {

    List<StudentCategoryResponse> findAll();

    StudentCategoryResponse create(StudentCategoryRequest request);

    StudentCategoryResponse update(UUID id, StudentCategoryRequest request);

    /** Refuses to delete a category that learners are still assigned to. */
    void delete(UUID id);
}
