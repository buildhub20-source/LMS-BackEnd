package com.lms.student.service;

import com.lms.common.response.PageResponse;
import com.lms.student.dto.request.CreateStudentRequest;
import com.lms.student.dto.request.GeneratePhotoUploadUrlRequest;
import com.lms.student.dto.request.UpdateStudentRequest;
import com.lms.student.dto.response.StudentPhotoUploadUrlResponse;
import com.lms.student.dto.response.StudentReferenceDataResponse;
import com.lms.student.dto.response.StudentResponse;
import com.lms.student.entity.EnrolmentStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Learner records for a training centre or college.
 *
 * <p>Admitting a learner also creates the account behind it: the profile is
 * meaningless without a {@code users} row, and a learner who cannot sign in is
 * not much use. Account creation goes through the invitation flow so the
 * credential path stays in one place.
 */
public interface StudentService {

    StudentResponse create(CreateStudentRequest request);

    StudentResponse findById(UUID id);

    PageResponse<StudentResponse> search(String search, UUID batchId,
                                         EnrolmentStatus enrolmentStatus, Pageable pageable);

    StudentResponse update(UUID id, UpdateStudentRequest request);

    /** Removes the learner record and the account created alongside it. */
    void delete(UUID id);

    StudentPhotoUploadUrlResponse generatePhotoUploadUrl(GeneratePhotoUploadUrlRequest request);

    /** Dropdown options for the intake form. */
    StudentReferenceDataResponse referenceData();
}
