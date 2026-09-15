package com.lms.gamification.service;

import com.lms.common.exception.ResourceNotFoundException;
import com.lms.gamification.dto.request.*;
import com.lms.gamification.dto.response.*;
import com.lms.gamification.entity.*;
import com.lms.gamification.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GamificationAdminService")
class GamificationAdminServiceTest {

    @Mock private BadgeRepository badgeRepository;
    @Mock private GamificationLevelRepository levelRepository;
    @Mock private MilestoneRepository milestoneRepository;
    @Mock private PointRuleRepository pointRuleRepository;

    @InjectMocks
    private GamificationAdminService adminService;

    @Test
    @DisplayName("creates and lists badges")
    void createAndListBadges() {
        CreateBadgeRequest req = new CreateBadgeRequest(
                "Speed Runner", "Completed assessment in under 10m",
                "zap", "ASSESSMENT", "ASSESSMENT_TIME", 600, true
        );

        Badge saved = Badge.builder()
                .id(UUID.randomUUID())
                .name(req.name())
                .description(req.description())
                .icon(req.icon())
                .category(req.category())
                .criteriaType(req.criteriaType())
                .criteriaValue(req.criteriaValue())
                .active(true)
                .build();

        when(badgeRepository.save(any(Badge.class))).thenReturn(saved);
        when(badgeRepository.findAll()).thenReturn(List.of(saved));

        BadgeResponse created = adminService.createBadge(req);
        assertThat(created.name()).isEqualTo("Speed Runner");

        List<BadgeResponse> list = adminService.listBadges();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).name()).isEqualTo("Speed Runner");
    }

    @Test
    @DisplayName("updates point rule")
    void updatePointRule() {
        UUID ruleId = UUID.randomUUID();
        PointRule existing = PointRule.builder()
                .id(ruleId)
                .eventType("LESSON_COMPLETION")
                .points(10)
                .active(true)
                .build();

        when(pointRuleRepository.findById(ruleId)).thenReturn(Optional.of(existing));
        when(pointRuleRepository.save(existing)).thenReturn(existing);

        UpdatePointRuleRequest req = new UpdatePointRuleRequest(25, true);
        PointRuleResponse updated = adminService.updatePointRule(ruleId, req);

        assertThat(updated.points()).isEqualTo(25);
        verify(pointRuleRepository).save(existing);
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when deleting non-existent badge")
    void throwsWhenDeletingNonExistentBadge() {
        UUID fakeId = UUID.randomUUID();
        when(badgeRepository.existsById(fakeId)).thenReturn(false);

        assertThatThrownBy(() -> adminService.deleteBadge(fakeId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
