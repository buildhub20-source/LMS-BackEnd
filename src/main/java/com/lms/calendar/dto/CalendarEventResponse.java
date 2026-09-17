package com.lms.calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * A single calendar event returned to the student.
 * Events can be assessment deadlines, course milestones, or certificate expiries.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventResponse {

    private UUID id;

    /** ASSESSMENT_DEADLINE, ASSESSMENT_START, COURSE_ENROLLED, COURSE_COMPLETED */
    private String type;

    private String title;

    private String description;

    /** When the event starts (e.g. assessment window opens). */
    private Instant startTime;

    /** When the event ends / is due (e.g. assessment window closes). */
    private Instant endTime;

    /** The related entity id (assessmentId or courseId). */
    private UUID referenceId;

    /** A colour hint for the frontend: blue, green, amber, red, violet */
    private String color;

    /** Status hint: upcoming, active, overdue, completed */
    private String status;
}
