package com.lms.user.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.common.response.PageResponse;
import com.lms.security.authentication.AuthenticationService;
import com.lms.user.dto.request.ChangePasswordRequest;
import com.lms.user.dto.request.LockUserRequest;
import com.lms.user.dto.request.UpdateUserRequest;
import com.lms.user.dto.request.UpdateUserRolesRequest;
import com.lms.user.dto.response.AccountStatusHistoryResponse;
import com.lms.user.dto.response.UserResponse;
import com.lms.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * User management endpoints.
 *
 * <p>Accounts are created through {@code POST /invitations}, not here.
 */
@Tag(name = "Users")
@RestController
@RequestMapping(ApiPaths.USERS)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Search users")
    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.of(userService.search(search, active, pageable)));
    }

    @Operation(summary = "Fetch one user")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW') or #id == authentication.principal.userId")
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(userService.findById(id)));
    }

    @Operation(summary = "Update a profile")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE') or #id == authentication.principal.userId")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.of(userService.update(id, request)));
    }

    @Operation(summary = "Replace the roles assigned to a user")
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_MANAGE_ROLES')")
    public ResponseEntity<ApiResponse<UserResponse>> updateRoles(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserRolesRequest request) {

        return ResponseEntity.ok(ApiResponse.of(userService.updateRoles(id, request), "Roles updated"));
    }

    @Operation(summary = "Change your own password")
    @PostMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changeOwnPassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        UUID currentUserId = AuthenticationService.requirePrincipal().getUserId();
        userService.changePassword(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.message("Password updated. Please sign in again."));
    }

    @Operation(summary = "Deactivate an account")
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<ApiResponse<UserResponse>> deactivate(
            @PathVariable UUID id, @Valid @RequestBody(required = false) LockUserRequest request) {

        return ResponseEntity.ok(ApiResponse.of(userService.deactivate(id, reason(request))));
    }

    @Operation(summary = "Reactivate an account")
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> activate(
            @PathVariable UUID id, @Valid @RequestBody(required = false) LockUserRequest request) {

        return ResponseEntity.ok(ApiResponse.of(userService.activate(id, reason(request))));
    }

    @Operation(summary = "Lock an account")
    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('USER_LOCK')")
    public ResponseEntity<ApiResponse<UserResponse>> lock(
            @PathVariable UUID id, @Valid @RequestBody(required = false) LockUserRequest request) {

        return ResponseEntity.ok(ApiResponse.of(userService.lock(id, reason(request))));
    }

    @Operation(summary = "Unlock an account")
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('USER_LOCK')")
    public ResponseEntity<ApiResponse<UserResponse>> unlock(
            @PathVariable UUID id, @Valid @RequestBody(required = false) LockUserRequest request) {

        return ResponseEntity.ok(ApiResponse.of(userService.unlock(id, reason(request))));
    }

    @Operation(summary = "Account status history")
    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<ApiResponse<List<AccountStatusHistoryResponse>>> statusHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(userService.statusHistory(id)));
    }

    private String reason(LockUserRequest request) {
        return request == null ? null : request.getReason();
    }
}
