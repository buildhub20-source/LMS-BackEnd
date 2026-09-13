package com.lms.platform.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.platform.dto.BroadcastAnnouncementDto;
import com.lms.platform.dto.CreateAnnouncementRequest;
import com.lms.platform.security.PlatformAdminPrincipal;
import com.lms.platform.service.PlatformAnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping(ApiPaths.PLATFORM + "/announcements")
@RequiredArgsConstructor
public class PlatformAnnouncementController {

    private final PlatformAnnouncementService announcementService;

    @GetMapping
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<List<BroadcastAnnouncementDto>>> listAll() {
        return ResponseEntity.ok(ApiResponse.of(announcementService.listAll()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<BroadcastAnnouncementDto>>> listActive() {
        return ResponseEntity.ok(ApiResponse.of(announcementService.listActive()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<BroadcastAnnouncementDto>> create(
            @Valid @RequestBody CreateAnnouncementRequest request,
            @AuthenticationPrincipal PlatformAdminPrincipal admin) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(announcementService.create(request, admin.id())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        announcementService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<BroadcastAnnouncementDto>> toggle(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.of(announcementService.toggleActive(id, active)));
    }
}
