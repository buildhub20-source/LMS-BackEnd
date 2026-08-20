package com.lms.invitation.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.common.response.PageResponse;
import com.lms.invitation.dto.request.CreateInvitationRequest;
import com.lms.invitation.dto.response.InvitationResponse;
import com.lms.invitation.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Invitation endpoints.
 *
 * <p>Onboarding is temporary-password only: the invitee signs in with the
 * password emailed to them and is forced to replace it. There is deliberately
 * no public accept-by-link endpoint. See docs/api/authentication.md for what
 * re-introducing one would involve.
 */
@Tag(name = "Invitations")
@Validated
@RestController
@RequestMapping(ApiPaths.INVITATIONS)
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @Operation(summary = "Create an account and email its temporary password")
    @PostMapping
    @PreAuthorize("hasAuthority('INVITATION_CREATE')")
    public ResponseEntity<ApiResponse<InvitationResponse>> invite(
            @Valid @RequestBody CreateInvitationRequest request) {

        InvitationResponse created = invitationService.invite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created, "Invitation sent"));
    }

    @Operation(summary = "List invitations")
    @GetMapping
    @PreAuthorize("hasAuthority('INVITATION_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<InvitationResponse>>> findAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(invitationService.findAll(pageable)));
    }

    @Operation(summary = "Fetch one invitation")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVITATION_VIEW')")
    public ResponseEntity<ApiResponse<InvitationResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(invitationService.findById(id)));
    }

    @Operation(summary = "Issue a fresh temporary password and send the invitation again")
    @PostMapping("/{id}/resend")
    @PreAuthorize("hasAuthority('INVITATION_MANAGE')")
    public ResponseEntity<ApiResponse<InvitationResponse>> resend(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(invitationService.resend(id), "Invitation resent"));
    }

    @Operation(summary = "Withdraw an unaccepted invitation and its pending account")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVITATION_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable UUID id) {
        invitationService.revoke(id);
        return ResponseEntity.ok(ApiResponse.message("Invitation revoked"));
    }
}
