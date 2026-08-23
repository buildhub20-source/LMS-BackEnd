package com.lms.auth.controller;

import com.lms.auth.dto.request.AcceptInvitationRequest;
import com.lms.auth.dto.request.ForgotPasswordRequest;
import com.lms.auth.dto.request.LoginRequest;
import com.lms.auth.dto.request.LogoutRequest;
import com.lms.auth.dto.request.RefreshTokenRequest;
import com.lms.auth.dto.request.ResetPasswordRequest;
import com.lms.auth.dto.response.AuthTokens;
import com.lms.auth.dto.response.CurrentUserResponse;
import com.lms.auth.dto.response.LoginResponse;
import com.lms.auth.dto.response.SessionResponse;
import com.lms.auth.service.AuthService;
import com.lms.auth.service.PasswordResetService;
import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Authentication endpoints. */
@Tag(name = "Authentication")
@RestController
@RequestMapping(ApiPaths.AUTH)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "Sign in and receive an access token plus a refresh token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.of(authService.login(request)));
    }

    @Operation(summary = "Accept a magic-link invitation and set a permanent password",
            description = "Public endpoint — no token required. Validates the invitation token, " +
                    "sets the user\'s password, and returns a full login response.")
    @PostMapping("/accept-invitation")
    public ResponseEntity<ApiResponse<LoginResponse>> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request) {
        return ResponseEntity.ok(ApiResponse.of(authService.acceptInvitation(request)));
    }

    @Operation(summary = "Rotate the refresh token and receive a new token pair")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokens>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.of(authService.refresh(request)));
    }

    @Operation(summary = "Revoke the current session")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.message("Signed out"));
    }

    @Operation(summary = "Revoke every session belonging to the current user")
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutEverywhere() {
        int revoked = authService.logoutEverywhere();
        return ResponseEntity.ok(ApiResponse.message("Revoked " + revoked + " session(s)"));
    }

    @Operation(summary = "The authenticated user with roles and permissions")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> currentUser() {
        return ResponseEntity.ok(ApiResponse.of(authService.currentUser()));
    }

    @Operation(summary = "List the live sessions of the current user")
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> mySessions() {
        return ResponseEntity.ok(ApiResponse.of(authService.mySessions()));
    }

    @Operation(summary = "Revoke one of the current user sessions")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable UUID sessionId) {
        authService.revokeSession(sessionId);
        return ResponseEntity.ok(ApiResponse.message("Session revoked"));
    }

    @Operation(summary = "Request a password reset link",
            description = "Always returns success so the endpoint cannot be used to discover accounts")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request);
        return ResponseEntity.ok(ApiResponse.message(
                "If that address has an account, a reset link has been sent"));
    }

    @Operation(summary = "Set a new password using a reset token")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.message("Password updated. Please sign in again."));
    }
}
