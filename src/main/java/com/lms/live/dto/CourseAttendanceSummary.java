package com.lms.live.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Live-class attendance for one course. Only classes that actually took place
 * (ended, or live now) count; missed and cancelled classes do not.
 *
 * @param attendanceRate average of the students' attendance percentages, 0–100
 */
public record CourseAttendanceSummary(
        UUID courseId,
        String courseTitle,
        int classesHeld,
        int enrolledStudents,
        int studentsAttendedAny,
        double attendanceRate,
        List<StudentRow> students,
        List<ClassRow> classes
) {
    public record StudentRow(
            UUID studentId,
            String name,
            String email,
            int classesAttended,
            int classesHeld,
            double percentage,
            long minutesAttended
    ) {}

    public record ClassRow(
            UUID sessionId,
            String title,
            Instant scheduledStart,
            String status,
            int studentsAttended,
            int enrolledStudents
    ) {}
}
