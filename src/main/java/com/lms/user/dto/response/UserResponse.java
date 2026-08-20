package com.lms.user.dto.response;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Public view of a user account. The password hash is never mapped into it. */
public class UserResponse {

    private UUID id;

    private String name;

    private String email;

    private String phone;

    private String profileImageUrl;

    private boolean active;

    private boolean locked;

    private boolean activated;

    private Set<String> roles;

    private Instant createdAt;

    public UserResponse() {
    }

    public UserResponse(UUID id, String name, String email, String phone, String profileImageUrl, boolean active, boolean locked, boolean activated, Set<String> roles, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
        this.active = active;
        this.locked = locked;
        this.activated = activated;
        this.roles = roles;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
