package com.lms.gamification.dto.response;

import java.time.LocalDate;

public record StreakResponse(
        int currentStreak,
        int longestStreak,
        LocalDate lastActivityDate
) {}
