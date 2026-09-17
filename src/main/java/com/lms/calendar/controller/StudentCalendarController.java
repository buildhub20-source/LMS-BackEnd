package com.lms.calendar.controller;

import com.lms.calendar.dto.CalendarEventResponse;
import com.lms.calendar.service.CalendarService;
import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.security.authentication.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.STUDENT_CALENDAR)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
@Transactional(readOnly = true)
public class StudentCalendarController {

    private final CalendarService calendarService;

    /**
     * Returns all calendar events for the authenticated student.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> getCalendarEvents() {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(calendarService.getStudentCalendarEvents(studentId)));
    }

    /**
     * Returns upcoming deadlines within the given number of days (default 30).
     */
    @GetMapping("/deadlines")
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> getUpcomingDeadlines(
            @RequestParam(required = false, defaultValue = "30") int days) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(calendarService.getUpcomingDeadlines(studentId, days)));
    }
}
