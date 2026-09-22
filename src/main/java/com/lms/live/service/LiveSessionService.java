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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public LiveSessionResponse createSession(UUID courseId, CreateLiveSessionRequest request, UUID actorId) {
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
        String roomName = liveKitService.buildRoomName(tenantId, sessionId);

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
                .status(LiveSessionStatus.SCHEDULED)
                .build();

        LiveSession saved = sessionRepository.save(session);
        log.info("Scheduled Live Session {} for course {}", saved.getId(), course.getTitle());
        return LiveSessionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<LiveSessionResponse> getCourseSessions(UUID courseId, UUID actorId) {
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

    @Transactional(readOnly = true)
    public List<LiveSessionResponse> getAllSessions(UUID courseId, String status, UUID actorId) {
        if (courseId != null) {
            return getCourseSessions(courseId, actorId);
        }

        UUID tenantId = resolveTenantId();
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

    @Transactional(readOnly = true)
    public LiveSessionResponse getSession(UUID sessionId, UUID actorId) {
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
                true
        );
    }

    @Transactional
    public JoinLiveSessionResponse joinSession(UUID sessionId, UUID actorId) {
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

        if (session.getStatus() != LiveSessionStatus.LIVE) {
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
                isInstructor || isAdmin
        );
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
        return tenantConfigRepository.findByTenantId(tenantId).orElse(null);
    }
}
