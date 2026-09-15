package com.lms.gamification.controller;

import com.lms.common.response.ApiResponse;
import com.lms.gamification.dto.request.*;
import com.lms.gamification.dto.response.*;
import com.lms.gamification.service.GamificationAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GamificationAdminController")
class GamificationAdminControllerTest {

    @Mock
    private GamificationAdminService adminService;

    @InjectMocks
    private GamificationAdminController controller;

    @Test
    @DisplayName("listBadges delegates to adminService")
    void listBadgesDelegates() {
        BadgeResponse badge = new BadgeResponse(
                UUID.randomUUID(), "Master", "Desc", "trophy",
                "COURSE", "COURSE_COUNT", 1, true, null
        );
        when(adminService.listBadges()).thenReturn(List.of(badge));

        ResponseEntity<ApiResponse<List<BadgeResponse>>> response = controller.listBadges();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
        verify(adminService).listBadges();
    }

    @Test
    @DisplayName("createBadge returns 201 CREATED status")
    void createBadgeReturnsCreated() {
        CreateBadgeRequest req = new CreateBadgeRequest(
                "Master", "Desc", "trophy", "COURSE", "COURSE_COUNT", 1, true
        );
        BadgeResponse badge = new BadgeResponse(
                UUID.randomUUID(), "Master", "Desc", "trophy",
                "COURSE", "COURSE_COUNT", 1, true, null
        );
        when(adminService.createBadge(req)).thenReturn(badge);

        ResponseEntity<ApiResponse<BadgeResponse>> response = controller.createBadge(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData().name()).isEqualTo("Master");
    }

    @Test
    @DisplayName("updatePointRule returns updated rule")
    void updatePointRuleReturnsData() {
        UUID ruleId = UUID.randomUUID();
        UpdatePointRuleRequest req = new UpdatePointRuleRequest(50, true);
        PointRuleResponse rule = new PointRuleResponse(ruleId, "LESSON_COMPLETION", 50, true);
        when(adminService.updatePointRule(eq(ruleId), any())).thenReturn(rule);

        ResponseEntity<ApiResponse<PointRuleResponse>> response = controller.updatePointRule(ruleId, req);

        assertThat(response.getBody().getData().points()).isEqualTo(50);
        verify(adminService).updatePointRule(ruleId, req);
    }
}
