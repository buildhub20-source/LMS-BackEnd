package com.lms.analytics;

import com.lms.analytics.controller.AnalyticsController;
import com.lms.assessment.repository.*;
import com.lms.course.entity.*;
import com.lms.course.repository.CourseRepository;
import com.lms.course.service.LearningTimeService;
import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.gamification.repository.*;
import com.lms.instructor.repository.InstructorProfileRepository;
import com.lms.invitation.repository.InvitationRepository;
import com.lms.student.repository.StudentProfileRepository;
import com.lms.user.repository.UserRepository;
import com.lms.security.authentication.LmsUserDetails;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class ProductionAnalyticsRegressionTest {
    private final CourseRepository courses = mock(CourseRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final LearningTimeService time = mock(LearningTimeService.class);
    private final InstructorProfileRepository instructors = mock(InstructorProfileRepository.class);
    private final StudentProfileRepository students = mock(StudentProfileRepository.class);
    private final AnalyticsController controller = new AnalyticsController(mock(UserRepository.class), courses,
            enrollments, mock(AssessmentRepository.class), mock(AssessmentAttemptRepository.class),
            mock(InvitationRepository.class), mock(LearningStreakRepository.class), mock(PointsLedgerRepository.class),
            time, instructors, students);

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test @SuppressWarnings("unchecked") void shortStudySessionsAndActiveEnrollmentCountsRemainAccurate() {
        var id = UUID.randomUUID();
        var user = new LmsUserDetails(id, "student@example.test", "Student", null, true, false, Set.of("STUDENT"), Set.of(), null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(enrollments.countByStudentId(id)).thenReturn(10L);
        when(enrollments.countByStudentIdAndStatus(id, EnrollmentStatus.COMPLETED)).thenReturn(3L);
        when(enrollments.countByStudentIdAndStatus(id, EnrollmentStatus.ACTIVE)).thenReturn(5L);
        var monday = LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        when(time.secondsByDay(any(), any(), any())).thenReturn(Map.of(monday, 20L));
        var data = controller.getStudentProgress().getBody().getData();
        assertThat(data).containsEntry("inProgressCount", 5L).containsEntry("completedCount", 3L);
        var trend = (List<Map<String, Object>>) data.get("enrollmentTrend");
        assertThat(trend.get(0)).containsEntry("seconds", 20L);
        assertThat((Double) trend.get(0).get("hours")).isGreaterThan(0);
    }

    @Test @SuppressWarnings("unchecked") void riskRequiresEnrollmentsAndBelowFiftyPercentCompletion() {
        var empty = course("No enrollments"); var risk = course("Needs attention"); var healthy = course("Healthy");
        when(courses.findAll()).thenReturn(List.of(empty, risk, healthy));
        when(enrollments.summarizeByCourse(EnrollmentStatus.COMPLETED)).thenReturn(List.of(
                new Object[]{risk.getId(), 10L, 4L}, new Object[]{healthy.getId(), 10L, 5L}));
        when(instructors.count()).thenReturn(7L); when(students.count()).thenReturn(12L);
        var data = controller.getAdminOverview().getBody().getData();
        assertThat(data).containsEntry("instructorCount", 7L).containsEntry("studentCount", 12L);
        var atRisk = (List<Map<String, Object>>) data.get("atRiskCourses");
        assertThat(atRisk).hasSize(1);
        assertThat(atRisk.get(0)).containsEntry("id", risk.getId()).containsEntry("completionRate", 40);
    }

    private Course course(String title) {
        var course = new Course(); course.setId(UUID.randomUUID()); course.setTitle(title); course.setStatus(CourseStatus.PUBLISHED); return course;
    }
}
