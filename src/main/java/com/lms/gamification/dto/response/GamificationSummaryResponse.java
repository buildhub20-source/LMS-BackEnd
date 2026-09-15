package com.lms.gamification.dto.response;

public record GamificationSummaryResponse(
        int totalPoints,
        LevelResponse currentLevel,
        int levelProgress,
        int currentStreak,
        int longestStreak,
        long badgeCount,
        long milestoneCount,
        Long leaderboardRank
) {}
