package com.lms.gamification.service;

import com.lms.gamification.dto.response.*;
import com.lms.gamification.entity.GamificationEventType;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Core gamification operations for students. */
public interface GamificationService {

    /** Awards points for an event if not already awarded (idempotent). */
    void awardPoints(UUID studentId, GamificationEventType eventType, UUID sourceEntityId);

    /** Checks all active badges and awards any that the student qualifies for. */
    void checkAndAwardBadges(UUID studentId);

    /** Checks all active milestones and awards any that the student qualifies for. */
    void checkAndAwardMilestones(UUID studentId);

    /** Records daily learning activity and updates the student's streak. */
    void recordDailyActivity(UUID studentId);

    GamificationSummaryResponse getSummary(UUID studentId);

    List<BadgeResponse> getStudentBadges(UUID studentId);

    List<MilestoneResponse> getStudentMilestones(UUID studentId);

    StreakResponse getStudentStreak(UUID studentId);

    com.lms.common.response.PageResponse<PointsHistoryResponse> getPointsHistory(UUID studentId, Pageable pageable);

    com.lms.common.response.PageResponse<LeaderboardEntryResponse> getLeaderboard(
            String period, UUID batchId, Pageable pageable);

    LeaderboardEntryResponse getStudentLeaderboardEntry(UUID studentId, String period, UUID batchId);
}
