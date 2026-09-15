package com.lms.gamification.controller;

import com.lms.common.response.ApiResponse;
import com.lms.common.response.PageResponse;
import com.lms.gamification.dto.response.*;
import com.lms.gamification.service.GamificationService;
import com.lms.security.authentication.LmsUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GamificationController")
class GamificationControllerTest {

    @Mock
    private GamificationService gamificationService;

    @InjectMocks
    private GamificationController controller;

    private UUID studentId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        LmsUserDetails principal = new LmsUserDetails(
                studentId, "student@lms.local", "Student User", "password",
                true, false, Set.of("STUDENT"), Set.of("GAMIFICATION_VIEW"), UUID.randomUUID()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getSummary returns student gamification summary")
    void getSummaryReturnsData() {
        GamificationSummaryResponse summary = new GamificationSummaryResponse(
                120, null, 40, 3, 5, 2L, 1L, 4L
        );
        when(gamificationService.getSummary(studentId)).thenReturn(summary);

        ResponseEntity<ApiResponse<GamificationSummaryResponse>> response = controller.getSummary();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().totalPoints()).isEqualTo(120);
        assertThat(response.getBody().getData().currentStreak()).isEqualTo(3);
    }

    @Test
    @DisplayName("getBadges returns student badges list")
    void getBadgesReturnsList() {
        BadgeResponse badge = new BadgeResponse(
                UUID.randomUUID(), "Course Master", "Finished course", "trophy",
                "COURSE", "COURSE_COUNT", 1, true, null
        );
        when(gamificationService.getStudentBadges(studentId)).thenReturn(List.of(badge));

        ResponseEntity<ApiResponse<List<BadgeResponse>>> response = controller.getBadges();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).name()).isEqualTo("Course Master");
    }

    @Test
    @DisplayName("getStreak returns streak details")
    void getStreakReturnsData() {
        StreakResponse streak = new StreakResponse(5, 10, LocalDate.now());
        when(gamificationService.getStudentStreak(studentId)).thenReturn(streak);

        ResponseEntity<ApiResponse<StreakResponse>> response = controller.getStreak();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().currentStreak()).isEqualTo(5);
        assertThat(response.getBody().getData().longestStreak()).isEqualTo(10);
    }

    @Test
    @DisplayName("getLeaderboard returns leaderboard entries with student entry")
    void getLeaderboardReturnsData() {
        PageResponse<LeaderboardEntryResponse> page = new PageResponse<>(List.of(), 0, 10, 0, 0, true);
        when(gamificationService.getLeaderboard(eq("ALL_TIME"), any(), any())).thenReturn(page);
        when(gamificationService.getStudentLeaderboardEntry(studentId, "ALL_TIME", null)).thenReturn(null);

        ResponseEntity<ApiResponse<Map<String, Object>>> response =
                controller.getLeaderboard("ALL_TIME", null, PageRequest.of(0, 10));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).containsKey("entries");
        assertThat(response.getBody().getData()).containsKey("currentStudent");
    }
}
