package com.lms.notification.mail;

import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Works out who a learning notification mail should go to.
 *
 * <p>Audiences differ by event because the data model differs. A live class
 * belongs to a course, so its audience is that course's active enrolments.
 * Assessments and toolkit resources are not tied to a course at all — any
 * student can see one once it is published — so theirs is every active student.
 *
 * <p>Separate from the listener so the reminder sweep, which has no event to
 * react to, can resolve an audience the same way.
 */
@Component
@RequiredArgsConstructor
public class LearningMailAudience {

    private static final String STUDENT_ROLE = "STUDENT";

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    /** Every active student in the tenant currently in scope. */
    @Transactional(readOnly = true)
    public List<User> activeStudents() {
        return userRepository.findUsersByRoleName(STUDENT_ROLE).stream()
                .filter(User::isActive)
                .toList();
    }

    /** Students holding an active enrolment in the given course. */
    @Transactional(readOnly = true)
    public List<User> enrolledStudents(UUID courseId) {
        return enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE).stream()
                .map(enrollment -> enrollment.getStudent())
                .filter(student -> student != null && student.isActive())
                .toList();
    }
}
