package com.lms.live.service;

import com.lms.common.exception.BusinessRuleException;
import com.lms.course.entity.Course;
import com.lms.course.entity.CourseStatus;
import com.lms.course.repository.CourseRepository;
import com.lms.live.dto.CreateLiveSessionRequest;
import com.lms.live.dto.LiveSessionResponse;
import com.lms.live.repository.LiveSessionRepository;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * A deployment without a LiveKit server (LIVEKIT_ENABLED=false) must refuse
 * built-in live classes rather than hand out tokens for a server that does not
 * exist, while Google Meet classes keep working.
 */
@SpringBootTest(properties = "lms.livekit.enabled=false")
@ActiveProfiles("test")
@Transactional
@DisplayName("scheduling with LiveKit switched off")
class LiveKitDisabledSessionTest {

    @MockBean
    private GoogleCalendarService googleCalendarService;

    @Autowired private LiveSessionService service;
    @Autowired private LiveSessionRepository sessions;
    @Autowired private CourseRepository courses;
    @Autowired private UserRepository users;

    private User instructor;
    private Course course;

    @BeforeEach
    void seed() {
        instructor = users.save(User.builder()
                .name("Teacher").email(UUID.randomUUID() + "@lms.test")
                .password("unused").active(true).build());
        course = courses.save(Course.builder()
                .title("Live course").createdBy(instructor.getId())
                .instructorId(instructor.getId())
                .status(CourseStatus.PUBLISHED).build());
    }

    private CreateLiveSessionRequest request(String mode) {
        Instant start = Instant.now().plus(Duration.ofDays(2));
        return new CreateLiveSessionRequest("Graph algorithms", null, start, start.plus(Duration.ofHours(1)), mode);
    }

    @Test
    @DisplayName("a built-in class is refused with a readable reason, and nothing is saved")
    void builtInClassRefused() {
        assertThatThrownBy(() -> service.createSession(course.getId(), request("live"), instructor.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Use Google Meet");
        assertThat(sessions.findByCourseIdOrderByScheduledStartDesc(course.getId())).isEmpty();
    }

    @Test
    @DisplayName("a Google Meet class is still scheduled")
    void meetClassStillWorks() {
        when(googleCalendarService.createMeetEvent(any(), any(), any(), any(), any()))
                .thenReturn(new GoogleCalendarService.MeetEvent("evt-1", "https://meet.google.com/abc-defg-hij"));

        LiveSessionResponse created = service.createSession(course.getId(), request("google_meet"), instructor.getId());

        assertThat(created.meetingUrl()).isEqualTo("https://meet.google.com/abc-defg-hij");
    }
}
