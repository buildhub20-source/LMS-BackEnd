package com.lms.auth.mapper;

import com.lms.auth.dto.response.CurrentUserResponse;
import com.lms.auth.dto.response.SessionResponse;
import com.lms.auth.entity.UserSession;
import com.lms.user.entity.User;
import com.lms.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds authentication-specific API contracts.
 *
 * <p>A hand-written component rather than a MapStruct interface because both
 * mappings are derivations rather than field-by-field copies.
 */
@Component
@RequiredArgsConstructor
public class AuthMapper {

    private final UserMapper userMapper;

    public CurrentUserResponse toCurrentUser(User user) {
        return new CurrentUserResponse(
                userMapper.toResponse(user),
                user.roleNames(),
                user.permissionNames());
    }

    public SessionResponse toSessionResponse(UserSession session, UUID currentSessionId) {
        return new SessionResponse(
                session.getId(),
                session.getIpAddress(),
                session.getUserAgent(),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                session.getExpiresAt(),
                Objects.equals(session.getId(), currentSessionId));
    }

    public List<SessionResponse> toSessionResponses(List<UserSession> sessions, UUID currentSessionId) {
        return sessions.stream()
                .map(session -> toSessionResponse(session, currentSessionId))
                .toList();
    }
}
