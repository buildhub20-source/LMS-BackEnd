package com.lms.role.dto.response;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class RoleResponse {

    private UUID id;

    private String name;

    private String description;

    private boolean systemRole;

    private Set<String> permissions;

    private Instant createdAt;

    public RoleResponse() {
    }

    public RoleResponse(UUID id, String name, String description, boolean systemRole, Set<String> permissions, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.systemRole = systemRole;
        this.permissions = permissions;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystemRole() {
        return systemRole;
    }

    public void setSystemRole(boolean systemRole) {
        this.systemRole = systemRole;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
