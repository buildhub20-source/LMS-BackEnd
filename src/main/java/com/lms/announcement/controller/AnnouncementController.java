package com.lms.announcement.controller;

import com.lms.announcement.dto.AnnouncementRequest;
import com.lms.announcement.dto.AnnouncementResponse;
import com.lms.announcement.service.AnnouncementService;
import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.security.authentication.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.ANNOUNCEMENTS)
@RequiredArgsConstructor
public class AnnouncementController {

    private static final String MANAGERS = "hasAnyRole('INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')";

    private final AnnouncementService announcementService;

    @GetMapping
    @PreAuthorize(MANAGERS)
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.of(
                announcementService.list(AuthenticationService.requirePrincipal())));
    }

    /** Announcements students should currently see. */
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> listActive() {
        return ResponseEntity.ok(ApiResponse.of(announcementService.listVisible()));
    }

    @PostMapping
    @PreAuthorize(MANAGERS)
    public ResponseEntity<ApiResponse<AnnouncementResponse>> create(
            @Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(
                announcementService.create(request, AuthenticationService.requirePrincipal())));
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize(MANAGERS)
    public ResponseEntity<ApiResponse<AnnouncementResponse>> toggle(
            @PathVariable UUID id, @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.of(
                announcementService.setActive(id, active, AuthenticationService.requirePrincipal())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(MANAGERS)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        announcementService.delete(id, AuthenticationService.requirePrincipal());
        return ResponseEntity.ok(ApiResponse.message("Announcement deleted"));
    }
}
