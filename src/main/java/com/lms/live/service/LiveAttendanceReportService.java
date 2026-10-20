package com.lms.live.service;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.course.entity.Course;
import com.lms.course.repository.CourseRepository;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.live.dto.CourseAttendanceSummary;
import com.lms.live.dto.MyAttendanceResponse;
import com.lms.live.entity.LiveSession;
import com.lms.live.entity.LiveSessionAttendance;
import com.lms.live.entity.LiveSessionStatus;
import com.lms.live.repository.LiveSessionAttendanceRepository;
import com.lms.live.repository.LiveSessionRepository;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Attendance reports built from live-class join/leave records. */
@Service
@RequiredArgsConstructor
public class LiveAttendanceReportService {

    private static final Set<String> MANAGE_ANY = Set.of("ADMIN", "SUPER_ADMIN");

    private final LiveSessionService liveSessionService;
    private final LiveSessionRepository sessionRepository;
    private final LiveSessionAttendanceRepository attendanceRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    /** A class counts once it has actually taken place. */
    static boolean held(LiveSession session) {
        return session.getStatus() == LiveSessionStatus.ENDED
                || (session.getStatus() == LiveSessionStatus.LIVE && session.getActualStart() != null);
    }

    /** Per-student and per-class attendance for one course; for its instructor and admins. */
    @Transactional
    public CourseAttendanceSummary courseSummary(UUID courseId, LmsUserDetails actor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ResourceNotFoundException.of("Course", courseId));
        boolean admin = actor.getRoles().stream().anyMatch(MANAGE_ANY::contains);
        boolean teaches = actor.getUserId().equals(course.getInstructorId())
                || actor.getUserId().equals(course.getCreatedBy());
        if (!admin && !teaches) {
            throw new ApplicationException(ErrorCode.ACCESS_DENIED,
                    "Only the course's instructor or an admin can see its attendance.");
        }
        liveSessionService.expireStaleSessions(liveSessionService.resolveTenantId());

        List<LiveSession> classes = sessionRepository.findByCourseIdOrderByScheduledStartDesc(courseId)
                .stream().filter(LiveAttendanceReportService::held).toList();

        // Enrolled = active or completed; instructors who join their own class are not students.
        Map<UUID, User> students = new LinkedHashMap<>();
        Stream.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED)
                .flatMap(status -> enrollmentRepository.findByCourseIdAndStatus(courseId, status).stream())
                .map(Enrollment::getStudent)
                .filter(Objects::nonNull)
                .forEach(student -> students.putIfAbsent(student.getId(), student));

        Map<UUID, Map<UUID, Long>> secondsBySessionAndStudent = new HashMap<>();
        if (!classes.isEmpty()) {
            for (LiveSessionAttendance a : attendanceRepository.findBySessionIds(
                    classes.stream().map(LiveSession::getId).toList())) {
                if (a.getUser() == null || !students.containsKey(a.getUser().getId())) continue;
                secondsBySessionAndStudent
                        .computeIfAbsent(a.getSession().getId(), k -> new HashMap<>())
                        .merge(a.getUser().getId(), secondsWithinClass(a), Long::sum);
            }
        }

        int held = classes.size();
        List<CourseAttendanceSummary.StudentRow> studentRows = new ArrayList<>();
        int attendedAny = 0;
        double percentageSum = 0;
        for (User student : students.values()) {
            int attended = 0;
            long seconds = 0;
            for (Map<UUID, Long> perStudent : secondsBySessionAndStudent.values()) {
                Long s = perStudent.get(student.getId());
                if (s != null) {
                    attended++;
                    seconds += s;
                }
            }
            double pct = percentage(attended, held);
            percentageSum += pct;
            if (attended > 0) attendedAny++;
            studentRows.add(new CourseAttendanceSummary.StudentRow(student.getId(), student.getName(),
                    student.getEmail(), attended, held, pct, Math.round(seconds / 60.0)));
        }
        studentRows.sort(Comparator.comparingDouble(CourseAttendanceSummary.StudentRow::percentage)
                .thenComparing(CourseAttendanceSummary.StudentRow::name, Comparator.nullsLast(String::compareToIgnoreCase)));

        List<CourseAttendanceSummary.ClassRow> classRows = classes.stream()
                .map(session -> new CourseAttendanceSummary.ClassRow(
                        session.getId(), session.getTitle(), session.getScheduledStart(),
                        session.getStatus().name(),
                        secondsBySessionAndStudent.getOrDefault(session.getId(), Map.of()).size(),
                        students.size()))
                .toList();

        double rate = students.isEmpty() || held == 0 ? 0 : round1(percentageSum / students.size());
        return new CourseAttendanceSummary(course.getId(), course.getTitle(), held, students.size(),
                attendedAny, rate, studentRows, classRows);
    }

    /** The signed-in student's attendance across the courses they are enrolled in. */
    @Transactional
    public MyAttendanceResponse myAttendance(UUID studentId) {
        liveSessionService.expireStaleSessions(liveSessionService.resolveTenantId());
        List<UUID> courseIds = enrollmentRepository.findActiveCourseIdsByStudentId(studentId);
        if (courseIds == null || courseIds.isEmpty()) {
            return new MyAttendanceResponse(0, 0, 0, 0, List.of());
        }
        List<LiveSession> classes = sessionRepository.findByCourseIdInOrderByScheduledStartDesc(courseIds)
                .stream().filter(LiveAttendanceReportService::held).toList();
        if (classes.isEmpty()) {
            return new MyAttendanceResponse(0, 0, 0, 0, List.of());
        }

        Map<UUID, Long> secondsBySession = attendanceRepository
                .findBySessionIds(classes.stream().map(LiveSession::getId).toList()).stream()
                .filter(a -> a.getUser() != null && studentId.equals(a.getUser().getId()))
                .collect(Collectors.groupingBy(a -> a.getSession().getId(),
                        Collectors.summingLong(LiveAttendanceReportService::secondsWithinClass)));

        List<MyAttendanceResponse.ClassRow> rows = classes.stream()
                .map(session -> {
                    Long seconds = secondsBySession.get(session.getId());
                    return new MyAttendanceResponse.ClassRow(session.getId(), session.getTitle(),
                            session.getCourse() != null ? session.getCourse().getId() : null,
                            session.getCourse() != null ? session.getCourse().getTitle() : null,
                            session.getScheduledStart(), session.getStatus().name(),
                            seconds != null, seconds == null ? 0 : Math.round(seconds / 60.0));
                })
                .toList();

        int attended = secondsBySession.size();
        long minutes = rows.stream().mapToLong(MyAttendanceResponse.ClassRow::minutesAttended).sum();
        return new MyAttendanceResponse(classes.size(), attended, percentage(attended, classes.size()), minutes, rows);
    }

    /**
     * Time a record spent inside the class's own window. Older records could run
     * on until someone ended the class days later; those must not inflate reports.
     */
    static long secondsWithinClass(LiveSessionAttendance a) {
        LiveSession session = a.getSession();
        java.time.Instant start = session.getActualStart() != null ? session.getActualStart() : session.getScheduledStart();
        java.time.Instant end = session.getActualEnd() != null ? session.getActualEnd() : session.getScheduledEnd();
        if (start == null || end == null || a.getJoinedAt() == null) return a.getDurationSeconds();
        // A class that ran on long past its slot still counts only its scheduled length plus the 2 h grace.
        java.time.Instant cap = session.getScheduledEnd().plus(LiveSessionService.LIVE_OVERRUN_GRACE);
        if (end.isAfter(cap)) end = cap;
        java.time.Instant from = a.getJoinedAt().isBefore(start) ? start : a.getJoinedAt();
        java.time.Instant left = a.getLeftAt() != null ? a.getLeftAt() : end;
        java.time.Instant to = left.isAfter(end) ? end : left;
        long window = java.time.Duration.between(from, to).getSeconds();
        return Math.max(0, Math.min(window, a.getDurationSeconds()));
    }

    private static double percentage(int part, int whole) {
        return whole == 0 ? 0 : round1(100.0 * part / whole);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
