package com.lms.gamification.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.common.response.PageResponse;
import com.lms.gamification.dto.response.*;
import com.lms.gamification.service.GamificationService;
import com.lms.security.authentication.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.GAMIFICATION)
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('GAMIFICATION_VIEW') or hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN', 'SUPER_ADMIN')")
@Transactional(readOnly = true)
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/me/summary")
    public ResponseEntity<ApiResponse<GamificationSummaryResponse>> getSummary() {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(gamificationService.getSummary(studentId)));
    }

    @GetMapping("/me/badges")
    public ResponseEntity<ApiResponse<List<BadgeResponse>>> getBadges() {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(gamificationService.getStudentBadges(studentId)));
    }

    @GetMapping("/me/milestones")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getMilestones() {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(gamificationService.getStudentMilestones(studentId)));
    }

    @GetMapping("/me/streak")
    public ResponseEntity<ApiResponse<StreakResponse>> getStreak() {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(gamificationService.getStudentStreak(studentId)));
    }

    @GetMapping("/me/points")
    public ResponseEntity<ApiResponse<PageResponse<PointsHistoryResponse>>> getPoints(Pageable pageable) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(gamificationService.getPointsHistory(studentId, pageable)));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLeaderboard(
            @RequestParam(required = false, defaultValue = "ALL_TIME") String period,
            @RequestParam(required = false) UUID batchId,
            Pageable pageable) {
        UUID studentId = AuthenticationService.requirePrincipal().getUserId();
        PageResponse<LeaderboardEntryResponse> entries = gamificationService.getLeaderboard(period, batchId, pageable);
        LeaderboardEntryResponse currentStudent = gamificationService.getStudentLeaderboardEntry(studentId, period, batchId);

        Map<String, Object> result = new HashMap<>();
        result.put("entries", entries);
        result.put("currentStudent", currentStudent);
        return ResponseEntity.ok(ApiResponse.of(result));
    }
}
