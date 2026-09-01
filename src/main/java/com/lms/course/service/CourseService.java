package com.lms.course.service;

import com.lms.common.response.PageResponse;
import com.lms.course.dto.request.CreateCourseRequest;
import com.lms.course.dto.request.RejectCourseRequest;
import com.lms.course.dto.request.UpdateCourseRequest;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Course management use cases. */
public interface CourseService {

    CourseResponse create(CreateCourseRequest request);

    CourseResponse findById(UUID id);

    PageResponse<CourseResponse> search(String search, CourseStatus status, Pageable pageable);

    CourseResponse update(UUID id, UpdateCourseRequest request);

    void delete(UUID id);

    // --- Lifecycle transitions ---

    /** Admin direct-publish: DRAFT or UNPUBLISHED → PUBLISHED. */
    CourseResponse publish(UUID id);

    /** PUBLISHED → UNPUBLISHED. */
    CourseResponse unpublish(UUID id);

    /** PUBLISHED or UNPUBLISHED → ARCHIVED. */
    CourseResponse archive(UUID id);

    /** Instructor submit: DRAFT → PENDING_REVIEW. */
    CourseResponse submit(UUID id);

    /** Admin approve: PENDING_REVIEW → PUBLISHED. */
    CourseResponse approve(UUID id);

    /** Admin reject: PENDING_REVIEW → DRAFT. */
    CourseResponse reject(UUID id, RejectCourseRequest request);

    /** Returns courses the given student is enrolled in (for /courses/mine). */
    Page<CourseResponse> getMyEnrolledCourses(UUID studentId, Pageable pageable);
}
