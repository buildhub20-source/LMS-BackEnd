package com.lms.platform.dto;

import java.time.Instant;

public record PlatformLoginResponse(String accessToken, Instant expiresAt) {}
