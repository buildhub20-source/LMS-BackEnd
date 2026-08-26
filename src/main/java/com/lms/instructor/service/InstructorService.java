package com.lms.instructor.service;

import com.lms.common.response.PageResponse;
import com.lms.instructor.dto.request.CreateInstructorRequest;
import com.lms.instructor.dto.request.UpdateInstructorRequest;
import com.lms.instructor.dto.response.InstructorReferenceDataResponse;
import com.lms.instructor.dto.response.InstructorResponse;
import com.lms.instructor.entity.EmploymentType;
import com.lms.student.dto.request.GeneratePhotoUploadUrlRequest;
import com.lms.student.dto.response.StudentPhotoUploadUrlResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Instructor records for a training centre or college.
 *
 * <p>Onboarding an instructor also creates the account behind it, through the
 * invitation flow, so the credential path stays in one place — the same shape as
 * {@link com.lms.student.service.StudentService}.
 */
public interface InstructorService {

    InstructorResponse create(CreateInstructorRequest request);

    /** Includes the batches they are assigned to. */
    InstructorResponse findById(UUID id);

    PageResponse<InstructorResponse> search(String search, EmploymentType employmentType,
                                            Pageable pageable);

    InstructorResponse update(UUID id, UpdateInstructorRequest request);

    /** Removes the instructor record and the account created alongside it. */
    void delete(UUID id);

    StudentPhotoUploadUrlResponse generatePhotoUploadUrl(GeneratePhotoUploadUrlRequest request);

    InstructorReferenceDataResponse referenceData();
}
