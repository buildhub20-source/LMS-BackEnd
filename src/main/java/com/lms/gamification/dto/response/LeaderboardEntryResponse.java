package com.lms.gamification.dto.response;

import java.util.UUID;

public record LeaderboardEntryResponse(
        long rank,
        UUID studentId,
        String studentName,
        int totalPoints,
        String levelTitle,
        long badgeCount
) {}
