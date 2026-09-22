package com.lms.live.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.live.dto.CreateLiveSessionRequest;
import com.lms.live.dto.JoinLiveSessionResponse;
import com.lms.live.dto.LiveAttendanceResponse;
import com.lms.live.dto.LiveSessionResponse;
import com.lms.live.service.LiveSessionService;
import com.lms.security.authentication.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LiveSessionController {

    private final LiveSessionService liveSessionService;

    @PostMapping(ApiPaths.COURSES + "/{courseId}/live-sessions")
    @PreAuthorize("hasAuthority('LIVE_SESSION_MANAGE') or hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LiveSessionResponse>> scheduleSession(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateLiveSessionRequest request
    ) {
        UUID actorId = AuthenticationService.requirePrincipal().getUserId();
        LiveSessionResponse response = liveSessionService.createSession(courseId, request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping(ApiPaths.COURSES + "/{courseId}/live-sessions")
    @PreAuthorize("hasAuthority('LIVE_SESSION_VIEW') or hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<LiveSessionResponse>>> getCourseSessions(@PathVariable UUID courseId) {
        UUID actorId = AuthenticationService.requirePrincipal().getUserId();
        List<LiveSessionResponse> responses = liveSessionService.getCourseSessions(courseId, actorId);
        return ResponseEntity.ok(ApiResponse.of(responses));
    }

    @GetMapping(ApiPaths.LIVE_SESSIONS)
    @PreAuthorize("hasAuthority('LIVE_SESSION_VIEW') or hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<LiveSessionResponse>>> getAllSessions(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) String status
    ) {
        UUID actorId = AuthenticationService.requirePrincipal().getUserId();
        List<LiveSessionResponse> responses = liveSessionService.getAllSessions(courseId, status, actorId);
        return ResponseEntity.ok(ApiResponse.of(responses));
    }

    @GetMapping(ApiPaths.LIVE_SESSIONS + "/{sessionId}")
    @PreAuthorize("hasAuthority('LIVE_SESSION_VIEW') or hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LiveSessionResponse>> getSession(@PathVariable UUID sessionId) {
        UUID actorId = AuthenticationService.requirePrincipal().getUserId();
        LiveSessionResponse response = liveSessionService.getSession(sessionId, actorId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping(ApiPaths.LIVE_SESSIONS + "/{sessionId}/start")
    @PreAuthorize("hasAuthority('LIVE_SESSION_MANAGE') or hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<JoinLiveSessionResponse>> startSession(@PathVariable UUID sessionId) {
        UUID actorId = AuthenticationService.requirePrincipal().getUserId();
        JoinLiveSessionResponse response = liveSessionService.startSession(sessionId, actorId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping(ApiPaths.LIVE_SESSIONS + "/{sessionId}/join")
    @PreAuthorize("hasAuthority('LIVE_SESSION_VIEW') or hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<JoinLiveSessionResponse>> joinSession(@PathVariable UUID sessionId) {
        UUID actorId = AuthenticationService.requirePrincipal().getUserId();
        JoinLiveSessionResponse response = liveSessionService.joinSession(sessionId, actorId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping(ApiPaths.LIVE_SESSIONS + "/{sessionId}/end")
    @PreAuthorize("hasAuthority('LIVE_SESSION_MANAGE') or hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LiveSessionResponse>> endSession(@PathVariable UUID sessionId) {
        UUID actorId = AuthenticationService.requirePrincipal().getUserId();
        LiveSessionResponse response = liveSessionService.endSession(sessionId, actorId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping(ApiPaths.LIVE_SESSIONS + "/{sessionId}/attendance")
    @PreAuthorize("hasAuthority('LIVE_SESSION_MANAGE') or hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<LiveAttendanceResponse>>> getSessionAttendance(@PathVariable UUID sessionId) {
        UUID actorId = AuthenticationService.requirePrincipal().getUserId();
        List<LiveAttendanceResponse> responses = liveSessionService.getSessionAttendance(sessionId, actorId);
        return ResponseEntity.ok(ApiResponse.of(responses));
    }
}
