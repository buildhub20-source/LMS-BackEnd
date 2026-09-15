package com.lms.gamification.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.gamification.dto.request.*;
import com.lms.gamification.dto.response.*;
import com.lms.gamification.service.GamificationAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.ADMIN_GAMIFICATION)
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('GAMIFICATION_MANAGE') or hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class GamificationAdminController {

    private final GamificationAdminService adminService;

    // ─── Badges ─────────────────────────────────────────────────────────────────

    @GetMapping("/badges")
    public ResponseEntity<ApiResponse<List<BadgeResponse>>> listBadges() {
        return ResponseEntity.ok(ApiResponse.of(adminService.listBadges()));
    }

    @PostMapping("/badges")
    public ResponseEntity<ApiResponse<BadgeResponse>> createBadge(@Valid @RequestBody CreateBadgeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(adminService.createBadge(request)));
    }

    @PutMapping("/badges/{id}")
    public ResponseEntity<ApiResponse<BadgeResponse>> updateBadge(
            @PathVariable UUID id, @Valid @RequestBody CreateBadgeRequest request) {
        return ResponseEntity.ok(ApiResponse.of(adminService.updateBadge(id, request)));
    }

    @DeleteMapping("/badges/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBadge(@PathVariable UUID id) {
        adminService.deleteBadge(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    // ─── Levels ─────────────────────────────────────────────────────────────────

    @GetMapping("/levels")
    public ResponseEntity<ApiResponse<List<LevelResponse>>> listLevels() {
        return ResponseEntity.ok(ApiResponse.of(adminService.listLevels()));
    }

    @PostMapping("/levels")
    public ResponseEntity<ApiResponse<LevelResponse>> createLevel(@Valid @RequestBody CreateLevelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(adminService.createLevel(request)));
    }

    @PutMapping("/levels/{id}")
    public ResponseEntity<ApiResponse<LevelResponse>> updateLevel(
            @PathVariable UUID id, @Valid @RequestBody CreateLevelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(adminService.updateLevel(id, request)));
    }

    @DeleteMapping("/levels/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLevel(@PathVariable UUID id) {
        adminService.deleteLevel(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    // ─── Milestones ─────────────────────────────────────────────────────────────

    @GetMapping("/milestones")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> listMilestones() {
        return ResponseEntity.ok(ApiResponse.of(adminService.listMilestones()));
    }

    @PostMapping("/milestones")
    public ResponseEntity<ApiResponse<MilestoneResponse>> createMilestone(@Valid @RequestBody CreateMilestoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(adminService.createMilestone(request)));
    }

    @PutMapping("/milestones/{id}")
    public ResponseEntity<ApiResponse<MilestoneResponse>> updateMilestone(
            @PathVariable UUID id, @Valid @RequestBody CreateMilestoneRequest request) {
        return ResponseEntity.ok(ApiResponse.of(adminService.updateMilestone(id, request)));
    }

    @DeleteMapping("/milestones/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(@PathVariable UUID id) {
        adminService.deleteMilestone(id);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    // ─── Point Rules ────────────────────────────────────────────────────────────

    @GetMapping("/point-rules")
    public ResponseEntity<ApiResponse<List<PointRuleResponse>>> listPointRules() {
        return ResponseEntity.ok(ApiResponse.of(adminService.listPointRules()));
    }

    @PutMapping("/point-rules/{id}")
    public ResponseEntity<ApiResponse<PointRuleResponse>> updatePointRule(
            @PathVariable UUID id, @Valid @RequestBody UpdatePointRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.of(adminService.updatePointRule(id, request)));
    }
}
