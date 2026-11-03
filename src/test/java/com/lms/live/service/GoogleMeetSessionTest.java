package com.lms.live.service;

import com.lms.common.exception.BusinessRuleException;
import com.lms.course.entity.Course;
import com.lms.course.entity.CourseStatus;
import com.lms.course.repository.CourseRepository;
import com.lms.live.dto.CreateLiveSessionRequest;
import com.lms.live.dto.LiveSessionResponse;
import com.lms.live.entity.LiveSession;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the Google Meet half of scheduling a live class.
 *
 * <p>{@link GoogleCalendarService} is mocked, so this proves our side of the
 * contract — that a Meet-mode session asks for an event, keeps the id and join
 * URL it gets back, and fails cleanly when the call does not succeed. It does
 * NOT prove Google accepts the request: that needs a real OAuth token and a
 * live API call, which no test should depend on.
 *
 * <p>With no tenant rows in the test database {@code resolveTenantId()} returns
 * null and the config is null, which is what leaves Meet mode available without
 * a provider being configured.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("scheduling a live class in Google Meet mode")
class GoogleMeetSessionTest {

    private static final String MEET_URL = "https://meet.google.com/abc-defg-hij";

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
        return new CreateLiveSessionRequest(
                "Graph algorithms", "Trees and traversal", start, start.plus(Duration.ofHours(1)), mode);
    }

    @Test
    @DisplayName("keeps the event id and join URL that Google hands back")
    void storesTheMeetLink() {
        when(googleCalendarService.createMeetEvent(any(), any(), any(), any(), any()))
                .thenReturn(new GoogleCalendarService.MeetEvent("evt-123", MEET_URL));

        LiveSessionResponse created = service.createSession(course.getId(), request("google_meet"), instructor.getId());

        assertThat(created.meetingUrl()).isEqualTo(MEET_URL);
        assertThat(created.sessionMode()).isEqualTo("google_meet");

        LiveSession saved = sessions.findById(created.id()).orElseThrow();
        assertThat(saved.getMeetingUrl()).isEqualTo(MEET_URL);
        assertThat(saved.getGoogleEventId()).isEqualTo("evt-123");
        // Meet sessions do not get a LiveKit room, so the name is derived instead.
        assertThat(saved.getRoomName()).startsWith("google-meet-");
    }

    @Test
    @DisplayName("passes the class title, description and times to the calendar")
    void forwardsTheClassDetails() {
        when(googleCalendarService.createMeetEvent(any(), any(), any(), any(), any()))
                .thenReturn(new GoogleCalendarService.MeetEvent("evt-123", MEET_URL));

        CreateLiveSessionRequest request = request("google_meet");
        service.createSession(course.getId(), request, instructor.getId());

        verify(googleCalendarService).createMeetEvent(
                any(), eq("Graph algorithms"), eq("Trees and traversal"),
                eq(request.scheduledStart()), eq(request.scheduledEnd()));
    }

    @Test
    @DisplayName("a calendar failure is reported as a readable error, and no class is left behind")
    void calendarFailureLeavesNothingBehind() {
        when(googleCalendarService.createMeetEvent(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("token expired"));

        assertThatThrownBy(() -> service.createSession(course.getId(), request("google_meet"), instructor.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Could not create the Google Meet link");

        // The raw cause must not reach the user, and the half-made class must
        // not be persisted.
        assertThat(sessions.findByCourseIdOrderByScheduledStartDesc(course.getId())).isEmpty();
    }

    @Test
    @DisplayName("a business rule from the calendar layer is passed through unchanged")
    void businessRulePropagates() {
        when(googleCalendarService.createMeetEvent(any(), any(), any(), any(), any()))
                .thenThrow(new BusinessRuleException("Connect Google Calendar first."));

        assertThatThrownBy(() -> service.createSession(course.getId(), request("google_meet"), instructor.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Connect Google Calendar first.");
    }

    @Test
    @DisplayName("a LiveKit class never touches the calendar")
    void liveKitModeSkipsTheCalendar() {
        LiveSessionResponse created = service.createSession(course.getId(), request("live"), instructor.getId());

        verify(googleCalendarService, never()).createMeetEvent(any(), any(), any(), any(), any());
        assertThat(created.meetingUrl()).isNull();

        LiveSession saved = sessions.findById(created.id()).orElseThrow();
        assertThat(saved.getGoogleEventId()).isNull();
        assertThat(saved.getRoomName()).doesNotStartWith("google-meet-");
    }

    @Test
    @DisplayName("an unsupported mode is refused before the calendar is called")
    void unsupportedModeRefused() {
        assertThatThrownBy(() -> service.createSession(course.getId(), request("zoom"), instructor.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Choose a live session mode");

        verify(googleCalendarService, never()).createMeetEvent(any(), any(), any(), any(), any());
        assertThat(List.copyOf(sessions.findByCourseIdOrderByScheduledStartDesc(course.getId()))).isEmpty();
    }
}
