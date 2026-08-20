package com.lms.invitation.dto.response;

import com.lms.invitation.entity.InvitationStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** An invitation. Never carries the token or its digest. */
public class InvitationResponse {

    private UUID id;

    private UUID userId;

    private String name;

    private String email;

    private Set<String> roles;

    private InvitationStatus status;

    private Instant expiresAt;

    private Instant acceptedAt;

    private String invitedBy;

    private Instant createdAt;

    public InvitationResponse() {
    }

    public InvitationResponse(UUID id, UUID userId, String name, String email, Set<String> roles, InvitationStatus status, Instant expiresAt, Instant acceptedAt, String invitedBy, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.roles = roles;
        this.status = status;
        this.expiresAt = expiresAt;
        this.acceptedAt = acceptedAt;
        this.invitedBy = invitedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public String getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(String invitedBy) {
        this.invitedBy = invitedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
