package com.lms.auth.service;

import com.lms.auth.dto.request.LoginRequest;
import com.lms.auth.dto.request.LogoutRequest;
import com.lms.auth.dto.request.RefreshTokenRequest;
import com.lms.auth.dto.response.AuthTokens;
import com.lms.auth.dto.response.CurrentUserResponse;
import com.lms.auth.dto.response.LoginResponse;
import com.lms.auth.dto.response.SessionResponse;

import java.util.List;
import java.util.UUID;

/** Authentication use cases exposed by the API. */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    /** Exchanges a refresh token for a new token pair, rotating the old one. */
    AuthTokens refresh(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    int logoutEverywhere();

    CurrentUserResponse currentUser();

    List<SessionResponse> mySessions();

    void revokeSession(UUID sessionId);
}
