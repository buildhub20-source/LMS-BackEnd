package com.lms.gamification.service;

import com.lms.gamification.dto.response.*;
import com.lms.gamification.entity.*;
import com.lms.gamification.repository.*;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GamificationServiceImpl")
class GamificationServiceImplTest {

    @Mock private PointRuleRepository pointRuleRepository;
    @Mock private PointsLedgerRepository pointsLedgerRepository;
    @Mock private BadgeRepository badgeRepository;
    @Mock private StudentBadgeRepository studentBadgeRepository;
    @Mock private GamificationLevelRepository levelRepository;
    @Mock private LearningStreakRepository learningStreakRepository;
    @Mock private StreakActivityRepository streakActivityRepository;
    @Mock private MilestoneRepository milestoneRepository;
    @Mock private StudentMilestoneRepository studentMilestoneRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private GamificationServiceImpl service;

    private UUID studentId;
    private UUID sourceId;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        sourceId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("awardPoints")
    class AwardPointsTests {

        @Test
        @DisplayName("awards points when active rule exists and not already awarded")
        void awardsPointsSuccessfully() {
            PointRule rule = PointRule.builder()
                    .eventType("LESSON_COMPLETION")
                    .points(10)
                    .active(true)
                    .build();

            when(pointsLedgerRepository.existsByIdempotencyKey(anyString())).thenReturn(false);
            when(pointRuleRepository.findByEventTypeAndActiveTrue("LESSON_COMPLETION"))
                    .thenReturn(Optional.of(rule));

            service.awardPoints(studentId, GamificationEventType.LESSON_COMPLETION, sourceId);

            ArgumentCaptor<PointsLedger> captor = ArgumentCaptor.forClass(PointsLedger.class);
            verify(pointsLedgerRepository).save(captor.capture());
            PointsLedger saved = captor.getValue();
            assertThat(saved.getStudentId()).isEqualTo(studentId);
            assertThat(saved.getPoints()).isEqualTo(10);
            assertThat(saved.getEventType()).isEqualTo("LESSON_COMPLETION");
            assertThat(saved.getSourceEntityId()).isEqualTo(sourceId);
        }

        @Test
        @DisplayName("is idempotent and does not award points twice for same event and source")
        void idempotentAwardPoints() {
            when(pointsLedgerRepository.existsByIdempotencyKey(anyString())).thenReturn(true);

            service.awardPoints(studentId, GamificationEventType.LESSON_COMPLETION, sourceId);

            verify(pointRuleRepository, never()).findByEventTypeAndActiveTrue(anyString());
            verify(pointsLedgerRepository, never()).save(any());
        }

        @Test
        @DisplayName("skips if no active point rule configured")
        void skipsWhenNoActiveRule() {
            when(pointsLedgerRepository.existsByIdempotencyKey(anyString())).thenReturn(false);
            when(pointRuleRepository.findByEventTypeAndActiveTrue("LESSON_COMPLETION"))
                    .thenReturn(Optional.empty());

            service.awardPoints(studentId, GamificationEventType.LESSON_COMPLETION, sourceId);

            verify(pointsLedgerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("checkAndAwardBadges")
    class BadgeTests {

        @Test
        @DisplayName("awards badge when student meets criteria and badge is not already awarded")
        void awardsBadgeWhenCriteriaMet() {
            UUID badgeId = UUID.randomUUID();
            Badge badge = Badge.builder()
                    .id(badgeId)
                    .name("First Step")
                    .description("Completed your first lesson")
                    .icon("award")
                    .category("LESSON")
                    .criteriaType("LESSONS_COMPLETED")
                    .criteriaValue(1)
                    .active(true)
                    .build();

            when(badgeRepository.findByActiveTrue()).thenReturn(List.of(badge));
            when(studentBadgeRepository.existsByStudentIdAndBadgeId(studentId, badgeId)).thenReturn(false);
            when(pointsLedgerRepository.countByStudentIdAndEventType(studentId, "LESSON_COMPLETION")).thenReturn(1L);

            PointRule badgeRule = PointRule.builder()
                    .eventType("BADGE_EARNED")
                    .points(15)
                    .active(true)
                    .build();
            when(pointRuleRepository.findByEventTypeAndActiveTrue("BADGE_EARNED")).thenReturn(Optional.of(badgeRule));

            service.checkAndAwardBadges(studentId);

            verify(studentBadgeRepository).save(any(StudentBadge.class));
            verify(pointsLedgerRepository).save(any(PointsLedger.class));
        }

        @Test
        @DisplayName("does not award badge if student already has it")
        void doesNotAwardDuplicateBadge() {
            UUID badgeId = UUID.randomUUID();
            Badge badge = Badge.builder()
                    .id(badgeId)
                    .name("First Step")
                    .criteriaType("LESSONS_COMPLETED")
                    .criteriaValue(1)
                    .active(true)
                    .build();

            when(badgeRepository.findByActiveTrue()).thenReturn(List.of(badge));
            when(studentBadgeRepository.existsByStudentIdAndBadgeId(studentId, badgeId)).thenReturn(true);

            service.checkAndAwardBadges(studentId);

            verify(studentBadgeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("recordDailyActivity")
    class StreakTests {

        @Test
        @DisplayName("increments streak when active on consecutive day")
        void incrementsStreakOnConsecutiveDay() {
            LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
            LocalDate yesterday = today.minusDays(1);

            LearningStreak streak = LearningStreak.builder()
                    .studentId(studentId)
                    .currentStreak(3)
                    .longestStreak(5)
                    .lastActivityDate(yesterday)
                    .build();

            when(streakActivityRepository.existsByStudentIdAndActivityDate(studentId, today)).thenReturn(false);
            when(learningStreakRepository.findByStudentId(studentId)).thenReturn(Optional.of(streak));

            service.recordDailyActivity(studentId);

            verify(streakActivityRepository).save(any(StreakActivity.class));
            verify(learningStreakRepository).save(streak);
            assertThat(streak.getCurrentStreak()).isEqualTo(4);
            assertThat(streak.getLastActivityDate()).isEqualTo(today);
        }

        @Test
        @DisplayName("idempotent when multiple activities occur on the same day")
        void ignoresMultipleActivitiesSameDay() {
            LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);

            when(streakActivityRepository.existsByStudentIdAndActivityDate(studentId, today)).thenReturn(true);

            service.recordDailyActivity(studentId);

            verify(streakActivityRepository, never()).save(any());
            verify(learningStreakRepository, never()).save(any());
        }

        @Test
        @DisplayName("resets streak to 1 when activity date is missed by more than weekend allowance")
        void resetsStreakOnMissedDay() {
            LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
            LocalDate fourDaysAgo = today.minusDays(4);

            LearningStreak streak = LearningStreak.builder()
                    .studentId(studentId)
                    .currentStreak(10)
                    .longestStreak(10)
                    .lastActivityDate(fourDaysAgo)
                    .build();

            when(streakActivityRepository.existsByStudentIdAndActivityDate(studentId, today)).thenReturn(false);
            when(learningStreakRepository.findByStudentId(studentId)).thenReturn(Optional.of(streak));

            service.recordDailyActivity(studentId);

            verify(learningStreakRepository).save(streak);
            assertThat(streak.getCurrentStreak()).isEqualTo(1);
            assertThat(streak.getLongestStreak()).isEqualTo(10);
            assertThat(streak.getLastActivityDate()).isEqualTo(today);
        }
    }

    @Nested
    @DisplayName("getSummary")
    class SummaryTests {

        @Test
        @DisplayName("computes summary with level progress and badges count")
        void returnsCompleteSummary() {
            GamificationLevel level = GamificationLevel.builder()
                    .levelNumber(2)
                    .title("Learner")
                    .minPoints(100)
                    .maxPoints(299)
                    .build();

            LearningStreak streak = LearningStreak.builder()
                    .currentStreak(4)
                    .longestStreak(7)
                    .build();

            when(pointsLedgerRepository.sumPointsByStudentId(studentId)).thenReturn(150);
            when(levelRepository.findFirstByMinPointsLessThanEqualOrderByMinPointsDesc(150))
                    .thenReturn(Optional.of(level));
            when(studentBadgeRepository.countByStudentId(studentId)).thenReturn(3L);
            when(studentMilestoneRepository.findByStudentId(studentId)).thenReturn(List.of());
            when(learningStreakRepository.findByStudentId(studentId)).thenReturn(Optional.of(streak));
            when(userRepository.findAll()).thenReturn(List.of());

            GamificationSummaryResponse summary = service.getSummary(studentId);

            assertThat(summary.totalPoints()).isEqualTo(150);
            assertThat(summary.badgeCount()).isEqualTo(3L);
            assertThat(summary.milestoneCount()).isEqualTo(0L);
            assertThat(summary.currentStreak()).isEqualTo(4);
            assertThat(summary.longestStreak()).isEqualTo(7);
            assertThat(summary.leaderboardRank()).isNull();
            assertThat(summary.currentLevel().title()).isEqualTo("Learner");
            assertThat(summary.levelProgress()).isGreaterThanOrEqualTo(0);
        }
    }
}
