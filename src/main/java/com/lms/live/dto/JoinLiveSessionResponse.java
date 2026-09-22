package com.lms.live.dto;

public record JoinLiveSessionResponse(
        String serverUrl,
        String token,
        String roomName,
        String participantIdentity,
        String participantName,
        boolean isPublisher
) {}
