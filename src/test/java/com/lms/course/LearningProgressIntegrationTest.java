package com.lms.course;

import com.lms.course.controller.LearningController;
import com.lms.course.dto.request.SaveLearningProgressRequest;
import com.lms.course.entity.*;
import com.lms.course.repository.*;
import com.lms.enrollment.entity.*;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import com.lms.common.exception.ApplicationException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LearningProgressIntegrationTest {
    @Autowired LearningController controller;
    @Autowired UserRepository users;
    @Autowired CourseRepository courses;
    @Autowired CourseModuleRepository modules;
    @Autowired LessonRepository lessons;
    @Autowired EnrollmentRepository enrollments;
    @Autowired EntityManager entityManager;
    UUID courseId;
    UUID lessonId;
    UUID studentId;

    @BeforeEach void seed() {
        User student = users.save(User.builder().name("Progress student")
                .email(UUID.randomUUID() + "@lms.test").password("unused").active(true).build());
        studentId = student.getId();
        Course course = courses.save(Course.builder().title("Progress course").createdBy(studentId)
                .status(CourseStatus.PUBLISHED).build());
        courseId = course.getId();
        CourseModule module = modules.save(CourseModule.builder().course(course).title("Module").build());
        Lesson lesson = lessons.save(Lesson.builder().module(module).title("Lesson").lessonType(LessonType.TEXT).build());
        lessonId = lesson.getId();
        enrollments.save(Enrollment.builder().student(student).course(course).build());
        var principal = LmsUserDetails.fromClaims(studentId, student.getEmail(), Set.of("STUDENT"), Set.of("COURSE_VIEW"), UUID.randomUUID());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void savesProgressInDatabaseAndComputesCompletion() {
        controller.saveProgress(courseId, new SaveLearningProgressRequest(Set.of(lessonId)));
        entityManager.flush();
        entityManager.clear();
        var response = controller.getProgress(courseId).getBody().getData();
        assertThat(response.get("percent")).isEqualTo(100);
        var enrollment = enrollments.findByStudentIdAndCourseId(studentId, courseId).orElseThrow();
        assertThat(enrollment.getCompletedLessonIds()).containsExactly(lessonId);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(enrollment.getCompletedAt()).isNotNull();
    }

    @Test void rejectsForeignLessonIds() {
        assertThatThrownBy(() -> controller.saveProgress(courseId,
                new SaveLearningProgressRequest(Set.of(UUID.randomUUID())))).isInstanceOf(ApplicationException.class);
    }

    @Test void rejectsSuspendedEnrollment() {
        enrollments.findByStudentIdAndCourseId(studentId, courseId).orElseThrow().setStatus(EnrollmentStatus.SUSPENDED);
        assertThatThrownBy(() -> controller.getProgress(courseId)).isInstanceOf(ApplicationException.class);
    }

    @Test void unmarkingLessonReopensCourse() {
        controller.saveProgress(courseId, new SaveLearningProgressRequest(Set.of(lessonId)));
        controller.saveProgress(courseId, new SaveLearningProgressRequest(Set.of()));
        assertThat(controller.getProgress(courseId).getBody().getData().get("percent")).isEqualTo(0);
        var enrollment = enrollments.findByStudentIdAndCourseId(studentId, courseId).orElseThrow();
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(enrollment.getCompletedAt()).isNull();
    }

    @Test void rejectsLessonWithWrongCoursePath() {
        Course other = courses.save(Course.builder().title("Other").createdBy(studentId).status(CourseStatus.PUBLISHED).build());
        enrollments.save(Enrollment.builder().student(users.getReferenceById(studentId)).course(other).build());
        assertThatThrownBy(() -> controller.getLesson(other.getId(), lessonId)).isInstanceOf(ApplicationException.class);
    }
}
