package com.lms.user.dto.response;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class UserProfileResponse {

    private UUID id;
    private String name;
    private String fullName;
    private String email;
    private String phone;
    private String jobTitle;
    private String bio;
    private String profileImageUrl;
    private String avatarUrl;
    private boolean active;
    private boolean locked;
    private Set<String> roles;
    private Instant createdAt;

    public UserProfileResponse() {
    }

    public UserProfileResponse(UUID id, String name, String email, String phone, String jobTitle, String bio, String profileImageUrl, boolean active, boolean locked, Set<String> roles, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.fullName = name;
        this.email = email;
        this.phone = phone;
        this.jobTitle = jobTitle;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
        this.avatarUrl = profileImageUrl;
        this.active = active;
        this.locked = locked;
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
        this.fullName = name;
    }

    public String getFullName() {
        return fullName != null ? fullName : name;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        if (this.name == null) this.name = fullName;
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

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
        this.avatarUrl = profileImageUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl != null ? avatarUrl : profileImageUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        if (this.profileImageUrl == null) this.profileImageUrl = avatarUrl;
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
