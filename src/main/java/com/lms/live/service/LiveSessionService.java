package com.lms.live.service;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.course.entity.Course;
import com.lms.course.repository.CourseRepository;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.entity.EnrollmentStatus;
import com.lms.live.dto.CreateLiveSessionRequest;
import com.lms.live.dto.JoinLiveSessionResponse;
import com.lms.live.dto.LiveAttendanceResponse;
import com.lms.live.dto.LiveSessionResponse;
import com.lms.live.entity.LiveSession;
import com.lms.live.entity.LiveSessionStatus;
import com.lms.live.event.LiveSessionScheduledEvent;
import com.lms.live.repository.LiveSessionAttendanceRepository;
import com.lms.live.repository.LiveSessionRepository;
import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantConfig;
import com.lms.platform.repository.TenantConfigRepository;
import com.lms.platform.repository.TenantRepository;
import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveSessionService {

    private final LiveSessionRepository sessionRepository;
    private final LiveSessionAttendanceRepository attendanceRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TenantConfigRepository tenantConfigRepository;
    private final TenantRepository tenantRepository;
    private final LiveKitService liveKitService;
    private final LiveUsageService liveUsageService;
    private final LiveAttendanceService liveAttendanceService;
    private final GoogleCalendarService googleCalendarService;
    private final com.lms.platform.service.CentralTenantConfigReader centralTenantConfigReader;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final PlatformTransactionManager transactionManager;

    /**
     * Schedules a class. Not {@code @Transactional} on purpose: the Google Calendar
     * call for a Meet class happens between two short transactions, so a slow
     * Google response never holds one of the few pooled database connections.
     * If saving fails after the Calendar event was created, the event is deleted
     * again so no orphaned meeting is left on the institute's calendar.
     */
    public LiveSessionResponse createSession(UUID courseId, CreateLiveSessionRequest request, UUID actorId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        PreparedSession prepared = tx.execute(status -> prepareSession(courseId, request, actorId));
        LiveSession session = prepared.session();

        GoogleCalendarService.MeetEvent meet = null;
        if ("google_meet".equals(request.sessionMode())) {
            try { meet = googleCalendarService.createMeetEvent(prepared.config(), request.title(),
                    request.description(), request.scheduledStart(), request.scheduledEnd());
            } catch (BusinessRuleException ex) { throw ex; }
            catch (Exception ex) { throw new BusinessRuleException("Could not create the Google Meet link. Check the Calendar connection and try again."); }
            session.setGoogleEventId(meet.eventId());
            session.setMeetingUrl(meet.meetingUrl());
        }

        try {
            return tx.execute(status -> {
                // Re-attach the course and instructor loaded in the first transaction.
                session.setCourse(courseRepository.findById(courseId)
                        .orElseThrow(() -> ResourceNotFoundException.of("Course", courseId)));
                session.setInstructor(userRepository.findById(actorId)
                        .orElseThrow(() -> ResourceNotFoundException.of("User", actorId)));
                LiveSession saved = sessionRepository.save(session);
                log.info("Scheduled Live Session {} for course {}", saved.getId(), saved.getCourse().getTitle());

                // Tells the enrolled students the class is on their calendar. The
                // reminder mails are not scheduled here: the sweep in
                // LiveSessionReminderScheduler picks the session up from its start time.
                eventPublisher.publishEvent(new LiveSessionScheduledEvent(saved.getId(), courseId));
                return LiveSessionResponse.from(saved);
            });
        } catch (RuntimeException ex) {
            if (meet != null) googleCalendarService.deleteEventQuietly(prepared.config(), meet.eventId());
            throw ex;
        }
    }

    private record PreparedSession(LiveSession session, TenantConfig config) {}

    private PreparedSession prepareSession(UUID courseId, CreateLiveSessionRequest request, UUID actorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ResourceNotFoundException.of("Course", courseId));
        User instructor = userRepository.findById(actorId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", actorId));

        if (!isAdmin(instructor) && !Objects.equals(course.getInstructorId(), actorId)) {
            throw new ApplicationException(ErrorCode.ACCESS_DENIED,
                    "Only the assigned instructor or an admin can schedule a session for this course.");
        }

        UUID tenantId = resolveTenantId();
        TenantConfig config = getTenantConfig(tenantId);

        if (config != null && !config.isLiveClassesEnabled()) {
            throw new BusinessRuleException("Live Classes are not enabled for this institution/tenant.");
        }

        if (request.scheduledEnd().isBefore(request.scheduledStart())) {
            throw new BusinessRuleException("Scheduled end time must be after start time.");
        }

        long durationMinutes = Duration.between(request.scheduledStart(), request.scheduledEnd()).toMinutes();
        int maxDuration = config != null ? config.getMaxLiveSessionDurationMinutes() : 120;
        if (durationMinutes > maxDuration) {
            throw new BusinessRuleException("Session duration exceeds the allowed limit of " + maxDuration + " minutes.");
        }

        UUID sessionId = UUID.randomUUID();
        if (!"live".equals(request.sessionMode()) && !"google_meet".equals(request.sessionMode())) {
            throw new BusinessRuleException("Choose a live session mode.");
        }
        if (config != null) {
            String allowed = "GOOGLE_MEET".equals(config.getLiveSessionProvider()) ? "google_meet" : "live";
            if (!allowed.equals(request.sessionMode())) {
                throw new BusinessRuleException("This session mode is not enabled for your organization.");
            }
        }
        if ("live".equals(request.sessionMode()) && !liveKitService.isEnabled()) {
            throw new BusinessRuleException(
                    "Built-in live classes are not available on this server. Use Google Meet for live classes.");
        }
        String roomName = "google_meet".equals(request.sessionMode()) ? "google-meet-" + sessionId
                : liveKitService.buildRoomName(tenantId, sessionId);

        LiveSession session = LiveSession.builder()
                .id(sessionId)
                .tenantId(tenantId)
                .course(course)
                .instructor(instructor)
                .title(request.title())
                .description(request.description())
                .scheduledStart(request.scheduledStart())
                .scheduledEnd(request.scheduledEnd())
                .roomName(roomName)
                .sessionMode(request.sessionMode())
                .status(LiveSessionStatus.SCHEDULED)
                .build();
        return new PreparedSession(session, config);
    }

    @Transactional
    public List<LiveSessionResponse> getCourseSessions(UUID courseId, UUID actorId) {
        expireStaleSessions(resolveTenantId());
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ResourceNotFoundException.of("Course", courseId));
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", actorId));
        assertCanViewCourse(course, actor);

        return sessionRepository.findByCourseIdOrderByScheduledStartDesc(courseId)
                .stream()
                .map(LiveSessionResponse::from)
                .toList();
    }

    @Transactional
    public List<LiveSessionResponse> getAllSessions(UUID courseId, String status, UUID actorId) {
        if (courseId != null) {
            return getCourseSessions(courseId, actorId);
        }

        UUID tenantId = resolveTenantId();
        expireStaleSessions(tenantId);
        User user = userRepository.findById(actorId).orElse(null);
        boolean admin = user != null && isAdmin(user);
        boolean instructor = user != null && isInstructor(user);

        List<LiveSession> sessions;
        if (admin) {
            sessions = sessionRepository.findByTenantIdOrderByScheduledStartDesc(tenantId);
        } else if (instructor) {
            sessions = sessionRepository.findByInstructorIdOrderByScheduledStartDesc(actorId);
        } else {
            List<UUID> enrolledCourseIds = enrollmentRepository.findActiveCourseIdsByStudentId(actorId);
            if (enrolledCourseIds != null && !enrolledCourseIds.isEmpty()) {
                sessions = sessionRepository.findByCourseIdInOrderByScheduledStartDesc(enrolledCourseIds);
            } else {
                sessions = List.of();
            }
        }

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                LiveSessionStatus targetStatus = LiveSessionStatus.valueOf(status.toUpperCase());
                sessions = sessions.stream()
                        .filter(s -> s.getStatus() == targetStatus)
                        .toList();
            } catch (IllegalArgumentException ignored) {
            }
        }

        return sessions.stream()
                .map(LiveSessionResponse::from)
                .toList();
    }

    @Transactional
    public LiveSessionResponse getSession(UUID sessionId, UUID actorId) {
        expireStaleSessions(resolveTenantId());
        LiveSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("LiveSession", sessionId));
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", actorId));
        assertCanViewCourse(session.getCourse(), actor);
        return LiveSessionResponse.from(session);
    }

    @Transactional(readOnly = true)
    public List<LiveAttendanceResponse> getSessionAttendance(UUID sessionId, UUID actorId) {
        LiveSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("LiveSession", sessionId));
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", actorId));
        if (!isAdmin(actor) && !Objects.equals(session.getInstructor().getId(), actorId)) {
            throw new ApplicationException(ErrorCode.ACCESS_DENIED,
                    "Only the session instructor or an admin can view attendance.");
        }
        return liveAttendanceService.getSessionAttendance(sessionId);
    }

    @Transactional
    public JoinLiveSessionResponse startSession(UUID sessionId, UUID actorId) {
        LiveSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("LiveSession", sessionId));
        User user = userRepository.findById(actorId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", actorId));

        boolean isInstructor = Objects.equals(session.getInstructor().getId(), actorId);
        boolean isAdmin = isAdmin(user);

        if (!isInstructor && !isAdmin) {
            throw new ApplicationException(ErrorCode.ACCESS_DENIED, "Only the designated instructor or admin can start this session.");
        }

        UUID tenantId = session.getTenantId() != null ? session.getTenantId() : resolveTenantId();
        expireStaleSessions(tenantId);
        if (session.getStatus() == LiveSessionStatus.MISSED || session.getStatus() == LiveSessionStatus.ENDED) {
            throw new BusinessRuleException("This class's time has passed. Schedule a new class to go live.");
        }
        TenantConfig config = getTenantConfig(tenantId);

        if (config != null && !config.isLiveClassesEnabled()) {
            throw new BusinessRuleException("Live Classes are not enabled for this tenant.");
        }

        if (liveUsageService.isQuotaExceeded(tenantId)) {
            throw new BusinessRuleException("Monthly live participant-minute quota has been exceeded for this organization.");
        }

        int maxConcurrent = config != null ? config.getMaxConcurrentLiveSessions() : 2;
        long activeSessions = sessionRepository.countActiveSessionsByTenantId(tenantId);
        if (session.getStatus() != LiveSessionStatus.LIVE && activeSessions >= maxConcurrent) {
            throw new BusinessRuleException("Maximum concurrent live classes limit (" + maxConcurrent + ") reached. Please end an active class first.");
        }

        if (session.getStatus() == LiveSessionStatus.SCHEDULED) {
            session.setStatus(LiveSessionStatus.LIVE);
            session.setActualStart(Instant.now());
            sessionRepository.save(session);
            liveUsageService.recordSessionHosted(tenantId);
        }

        if (session.getMeetingUrl() != null) {
            return new JoinLiveSessionResponse(null, null, null, user.getId().toString(), user.getName(), true, session.getMeetingUrl());
        }

        liveAttendanceService.recordParticipantJoined(session, user);

        String token = liveKitService.generateParticipantToken(
                session.getRoomName(),
                user.getId().toString(),
                user.getName(),
                true,
                config == null || config.isScreenShareEnabled()
        );

        return new JoinLiveSessionResponse(
                liveKitService.getServerUrl(),
                token,
                session.getRoomName(),
                user.getId().toString(),
                user.getName(),
                true,
                null
        );
    }

    @Transactional
    public JoinLiveSessionResponse joinSession(UUID sessionId, UUID actorId) {
        expireStaleSessions(resolveTenantId());
        LiveSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("LiveSession", sessionId));
        User user = userRepository.findById(actorId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", actorId));

        boolean isInstructor = Objects.equals(session.getInstructor().getId(), actorId);
        boolean isAdmin = isAdmin(user);

        if (!isInstructor && !isAdmin) {
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                    actorId, session.getCourse().getId(), EnrollmentStatus.ACTIVE);
            if (!isEnrolled) {
                throw new ApplicationException(ErrorCode.ACCESS_DENIED, "You must be enrolled in this course to join the live session.");
            }
        }

        if (session.getMeetingUrl() != null) {
            if (session.getStatus() != LiveSessionStatus.SCHEDULED && session.getStatus() != LiveSessionStatus.LIVE) {
                throw new BusinessRuleException("This live class is no longer available to join.");
            }
            liveAttendanceService.recordParticipantJoined(session, user);
            return new JoinLiveSessionResponse(null, null, null, user.getId().toString(), user.getName(),
                    isInstructor || isAdmin, session.getMeetingUrl());
        }

        if (session.getStatus() != LiveSessionStatus.LIVE) {
            if (session.getMeetingUrl() != null && (isInstructor || isAdmin)) return startSession(sessionId, actorId);
            if (isInstructor || isAdmin) {
                return startSession(sessionId, actorId);
            }
            throw new BusinessRuleException("This live class is not active currently (status: " + session.getStatus() + ").");
        }

        UUID tenantId = session.getTenantId() != null ? session.getTenantId() : resolveTenantId();
        TenantConfig config = getTenantConfig(tenantId);

        int maxParticipants = config != null ? config.getMaxLiveParticipants() : 50;
        long currentParticipants = attendanceRepository.countActiveParticipants(sessionId);
        if (currentParticipants >= maxParticipants && !isInstructor && !isAdmin) {
            throw new BusinessRuleException("The classroom has reached its maximum capacity of " + maxParticipants + " participants.");
        }

        liveAttendanceService.recordParticipantJoined(session, user);

        String token = liveKitService.generateParticipantToken(
                session.getRoomName(),
                user.getId().toString(),
                user.getName(),
                isInstructor || isAdmin,
                config == null || config.isScreenShareEnabled()
        );

        return new JoinLiveSessionResponse(
                liveKitService.getServerUrl(),
                token,
                session.getRoomName(),
                user.getId().toString(),
                user.getName(),
                isInstructor || isAdmin,
                null
        );
    }

    @Transactional
    public void leaveSession(UUID sessionId, UUID actorId) {
        LiveSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("LiveSession", sessionId));
        User user = userRepository.findById(actorId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", actorId));
        liveAttendanceService.recordParticipantLeft(session, user);
    }

    @Transactional
    public LiveSessionResponse endSession(UUID sessionId, UUID actorId) {
        LiveSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("LiveSession", sessionId));
        User user = userRepository.findById(actorId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", actorId));

        boolean isInstructor = Objects.equals(session.getInstructor().getId(), actorId);
        boolean isAdmin = isAdmin(user);

        if (!isInstructor && !isAdmin) {
            throw new ApplicationException(ErrorCode.ACCESS_DENIED, "Only the instructor or admin can end this session.");
        }

        if (session.getStatus() == LiveSessionStatus.LIVE) {
            session.setStatus(LiveSessionStatus.ENDED);
            session.setActualEnd(Instant.now());
            sessionRepository.save(session);

            // Close all active attendance records
            attendanceRepository.findBySessionIdOrderByJoinedAtAsc(sessionId).forEach(attendance -> {
                if (attendance.getLeftAt() == null) {
                    liveAttendanceService.recordParticipantLeft(session, attendance.getUser());
                }
            });
            log.info("Live Session {} ended by {}", sessionId, user.getEmail());
        }

        return LiveSessionResponse.from(session);
    }

    /** A class left live this long past its scheduled end is treated as forgotten. */
    static final Duration LIVE_OVERRUN_GRACE = Duration.ofHours(2);

    /**
     * Sessions never end on their own when nobody presses "End" (and the LiveKit
     * webhook is not always reachable), so stale ones are settled whenever
     * sessions are read or started:
     * <ul>
     *   <li>LIVE more than {@link #LIVE_OVERRUN_GRACE} past its scheduled end → ENDED;
     *       attendance is closed at the scheduled end, not now.</li>
     *   <li>SCHEDULED and its whole slot has passed → MISSED.</li>
     * </ul>
     */
    void expireStaleSessions(UUID tenantId) {
        if (tenantId == null) {
            return;
        }
        Instant now = Instant.now();
        for (LiveSession session : sessionRepository.findOpenEndedBefore(tenantId, now)) {
            if (session.getStatus() == LiveSessionStatus.SCHEDULED) {
                session.setStatus(LiveSessionStatus.MISSED);
                sessionRepository.save(session);
                log.info("Live session {} marked MISSED (never started)", session.getId());
            } else if (session.getStatus() == LiveSessionStatus.LIVE
                    && now.isAfter(session.getScheduledEnd().plus(LIVE_OVERRUN_GRACE))) {
                Instant endedAt = session.getActualStart() != null && session.getActualStart().isAfter(session.getScheduledEnd())
                        ? session.getActualStart()
                        : session.getScheduledEnd();
                session.setStatus(LiveSessionStatus.ENDED);
                session.setActualEnd(endedAt);
                sessionRepository.save(session);
                liveAttendanceService.closeOpenAttendance(session, endedAt);
                log.info("Live session {} ended automatically (still live {} after its scheduled end)",
                        session.getId(), LIVE_OVERRUN_GRACE);
            }
        }
    }

    private boolean isAdmin(User user) {
        if (user.getUserRoles() == null) return false;
        return user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole() != null &&
                        ("ADMIN".equalsIgnoreCase(ur.getRole().getName()) || "SUPER_ADMIN".equalsIgnoreCase(ur.getRole().getName())));
    }

    private boolean isInstructor(User user) {
        if (user.getUserRoles() == null) return false;
        return user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole() != null &&
                        "INSTRUCTOR".equalsIgnoreCase(ur.getRole().getName()));
    }

    private void assertCanViewCourse(Course course, User actor) {
        if (isAdmin(actor) || Objects.equals(course.getInstructorId(), actor.getId())) {
            return;
        }
        boolean activelyEnrolled = enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                actor.getId(), course.getId(), EnrollmentStatus.ACTIVE);
        if (!activelyEnrolled) {
            throw new ApplicationException(ErrorCode.ACCESS_DENIED,
                    "You do not have access to this course's live sessions.");
        }
    }

    public UUID resolveTenantId() {
        return TenantContext.current()
                .map(TenantConnection::tenantId)
                .orElseGet(() -> tenantRepository.findAll().stream()
                        .findFirst()
                        .map(Tenant::getId)
                        .orElse(null));
    }

    public TenantConfig getTenantConfig(UUID tenantId) {
        if (tenantId == null) return null;
        return centralTenantConfigReader.find(tenantId);
    }
}
