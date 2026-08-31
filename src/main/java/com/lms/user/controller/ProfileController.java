package com.lms.user.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.security.authentication.AuthenticationService;
import com.lms.user.dto.request.ChangePasswordRequest;
import com.lms.user.dto.request.UpdateProfileRequest;
import com.lms.user.dto.response.UserProfileResponse;
import com.lms.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Endpoints for managing authenticated user's profile and account settings. */
@Tag(name = "Profile & Account Management")
@RestController
@RequestMapping(ApiPaths.PROFILE)
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @Operation(summary = "Get current authenticated user profile")
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getOwnProfile() {
        UUID currentUserId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(userService.getProfile(currentUserId)));
    }

    @Operation(summary = "Update current user profile")
    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateOwnProfile(
            @Valid @RequestBody UpdateProfileRequest request) {

        UUID currentUserId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(userService.updateProfile(currentUserId, request), "Profile updated successfully"));
    }

    @Operation(summary = "Change own password")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changeOwnPassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        UUID currentUserId = AuthenticationService.requirePrincipal().getUserId();
        userService.changePassword(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.message("Password updated successfully. Please sign in again."));
    }

    @Operation(summary = "Upload profile picture / avatar")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadAvatar(
            @RequestPart(value = "file", required = false) MultipartFile filePart,
            @RequestParam(value = "file", required = false) MultipartFile fileParam) {

        MultipartFile file = filePart != null ? filePart : fileParam;
        UUID currentUserId = AuthenticationService.requirePrincipal().getUserId();
        return ResponseEntity.ok(ApiResponse.of(userService.updateAvatar(currentUserId, file), "Avatar updated successfully"));
    }
}
