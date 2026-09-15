package com.lms.gamification.dto.response;

import java.util.UUID;

public record LevelResponse(
        UUID id,
        int levelNumber,
        String title,
        int minPoints,
        Integer maxPoints,
        String icon,
        String color,
        int progress
) {}
