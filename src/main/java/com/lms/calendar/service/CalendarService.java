package com.lms.calendar.service;

import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentStatus;
import com.lms.assessment.repository.AssessmentRepository;
import com.lms.calendar.dto.CalendarEventResponse;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Aggregates calendar events from assessments, enrollments, and courses
 * into a unified timeline for the student calendar view.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final AssessmentRepository assessmentRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Returns all calendar events for the given student.
     * Includes assessment windows and enrollment milestones.
     */
    public List<CalendarEventResponse> getStudentCalendarEvents(UUID studentId) {
        List<CalendarEventResponse> events = new ArrayList<>();

        // 1. Assessment deadlines — all published assessments with time windows
        List<Assessment> assessments = assessmentRepository
                .findByStatusOrderByCreatedAtDesc(AssessmentStatus.PUBLISHED, PageRequest.of(0, 100))
                .getContent();

        Instant now = Instant.now();
        for (Assessment assessment : assessments) {
            if (assessment.getStartTime() == null && assessment.getEndTime() == null) {
                continue; // open-ended assessments have no calendar event
            }

            String status;
            if (assessment.getEndTime() != null && assessment.getEndTime().isBefore(now)) {
                status = "overdue";
            } else if (assessment.getStartTime() != null && assessment.getStartTime().isAfter(now)) {
                status = "upcoming";
            } else {
                status = "active";
            }

            events.add(CalendarEventResponse.builder()
                    .id(assessment.getId())
                    .type("ASSESSMENT_DEADLINE")
                    .title(assessment.getTitle())
                    .description("Duration: " + assessment.getDurationMinutes() + " min · "
                            + assessment.getTotalMarks() + " marks")
                    .startTime(assessment.getStartTime())
                    .endTime(assessment.getEndTime())
                    .referenceId(assessment.getId())
                    .color("blue")
                    .status(status)
                    .build());
        }

        // 2. Enrollment milestones — courses enrolled, started, completed
        List<Enrollment> enrollments = enrollmentRepository
                .findByStudentId(studentId, PageRequest.of(0, 50))
                .getContent();

        for (Enrollment enrollment : enrollments) {
            // Enrollment date
            events.add(CalendarEventResponse.builder()
                    .id(enrollment.getId())
                    .type("COURSE_ENROLLED")
                    .title("Enrolled: " + enrollment.getCourse().getTitle())
                    .description("Course enrollment")
                    .startTime(enrollment.getEnrolledAt())
                    .endTime(enrollment.getEnrolledAt())
                    .referenceId(enrollment.getCourse().getId())
                    .color("green")
                    .status("completed")
                    .build());

            // Completion date
            if (enrollment.getCompletedAt() != null) {
                events.add(CalendarEventResponse.builder()
                        .id(UUID.randomUUID())
                        .type("COURSE_COMPLETED")
                        .title("Completed: " + enrollment.getCourse().getTitle())
                        .description("Course completed")
                        .startTime(enrollment.getCompletedAt())
                        .endTime(enrollment.getCompletedAt())
                        .referenceId(enrollment.getCourse().getId())
                        .color("emerald")
                        .status("completed")
                        .build());
            }
        }

        // Sort by start time (nearest first)
        events.sort(Comparator.comparing(
                CalendarEventResponse::getStartTime,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        return events;
    }

    /**
     * Returns only upcoming events (start_time > now) for the deadline list view.
     */
    public List<CalendarEventResponse> getUpcomingDeadlines(UUID studentId, int days) {
        Instant now = Instant.now();
        Instant cutoff = now.plusSeconds((long) days * 86400);

        return getStudentCalendarEvents(studentId).stream()
                .filter(e -> e.getEndTime() != null && e.getEndTime().isAfter(now) && e.getEndTime().isBefore(cutoff))
                .toList();
    }
}
