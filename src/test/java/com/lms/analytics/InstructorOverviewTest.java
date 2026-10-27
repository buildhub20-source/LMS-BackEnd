package com.lms.analytics;

import com.lms.analytics.controller.AnalyticsController;
import com.lms.assessment.entity.AttemptStatus;
import com.lms.assessment.repository.AssessmentAttemptRepository;
import com.lms.assessment.repository.AssessmentRepository;
import com.lms.course.repository.CourseRepository;
import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.gamification.repository.LearningStreakRepository;
import com.lms.gamification.repository.PointsLedgerRepository;
import com.lms.invitation.repository.InvitationRepository;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The instructor dashboard must show the instructor's own numbers, never the institute's. */
class InstructorOverviewTest {

    private final UUID instructorId = UUID.randomUUID();
    private final CourseRepository courses = mock(CourseRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final AssessmentAttemptRepository attempts = mock(AssessmentAttemptRepository.class);
    private final AnalyticsController controller = new AnalyticsController(
            mock(UserRepository.class), courses, enrollments, mock(AssessmentRepository.class), attempts,
            mock(InvitationRepository.class), mock(LearningStreakRepository.class), mock(PointsLedgerRepository.class),
            mock(com.lms.course.service.LearningTimeService.class),
            mock(com.lms.instructor.repository.InstructorProfileRepository.class),
            mock(com.lms.student.repository.StudentProfileRepository.class));

    @BeforeEach
    void signInAsInstructor() {
        LmsUserDetails instructor = new LmsUserDetails(instructorId, "teacher@srp.edu.in", "Teacher", null,
                true, false, Set.of("INSTRUCTOR"), Set.of(), null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(instructor, null, instructor.getAuthorities()));

        // The rest of the institute has plenty of activity.
        when(courses.count()).thenReturn(40L);
        when(courses.countByStatus(any())).thenReturn(30L);
        when(enrollments.count()).thenReturn(900L);
        when(enrollments.countByStatus(any())).thenReturn(300L);
        when(enrollments.countDistinctStudentsByStatus(any())).thenReturn(500L);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aNewInstructorSeesZerosNotTheInstitutesTotals() {
        Map<String, Object> data = overview();

        assertThat(data).containsEntry("courseCount", 0L)
                .containsEntry("learnerCount", 0L)
                .containsEntry("totalEnrollments", 0L)
                .containsEntry("averageScore", 0)
                .containsEntry("pendingGradingCount", 0L);
    }

    @Test
    void scoresAndPendingGradingComeFromTheInstructorsOwnAssessments() {
        when(courses.countByInstructorId(instructorId)).thenReturn(3L);
        when(enrollments.countByCourseInstructorId(instructorId)).thenReturn(20L);
        when(enrollments.countDistinctStudentsByCourseInstructorIdAndStatus(instructorId, EnrollmentStatus.ACTIVE)).thenReturn(15L);
        when(enrollments.countByCourseInstructorIdAndStatus(instructorId, EnrollmentStatus.COMPLETED)).thenReturn(5L);
        when(attempts.averageScoreOnAssessmentsCreatedBy(instructorId, AttemptStatus.SUBMITTED)).thenReturn(71.6);
        when(attempts.countByAssessment_CreatedByAndStatusAndScoreIsNull(instructorId, AttemptStatus.SUBMITTED))
                .thenReturn(4L);

        Map<String, Object> data = overview();

        assertThat(data).containsEntry("courseCount", 3L)
                .containsEntry("learnerCount", 15L)
                .containsEntry("averageScore", 72)
                .containsEntry("pendingGradingCount", 4L);
    }

    @SuppressWarnings("ConstantConditions")
    private Map<String, Object> overview() {
        return controller.getInstructorOverview().getBody().getData();
    }
}
