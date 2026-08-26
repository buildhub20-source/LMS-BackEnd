package com.lms.enrollment.mapper;

import com.lms.enrollment.dto.response.EnrollmentResponse;
import com.lms.enrollment.entity.Enrollment;
import com.lms.user.entity.User;
import com.lms.course.entity.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    @Mapping(target = "student", source = "student")
    @Mapping(target = "course", source = "course")
    EnrollmentResponse toResponse(Enrollment enrollment);

    List<EnrollmentResponse> toResponseList(List<Enrollment> enrollments);

    default EnrollmentResponse.StudentSummary toStudentSummary(User user) {
        if (user == null) {
            return null;
        }
        return EnrollmentResponse.StudentSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getName())
                .build();
    }

    default EnrollmentResponse.CourseSummary toCourseSummary(Course course) {
        if (course == null) {
            return null;
        }
        return EnrollmentResponse.CourseSummary.builder()
                .id(course.getId())
                .title(course.getTitle())
                .instructorId(course.getInstructorId())
                .build();
    }
}
