package com.lms.role.dto.request;

import jakarta.validation.constraints.Size;

import java.util.Set;

public class UpdateRoleRequest {

    @Size(max = 255)
    private String description;

    private Set<String> permissions;

    public UpdateRoleRequest() {
    }

    public UpdateRoleRequest(String description, Set<String> permissions) {
        this.description = description;
        this.permissions = permissions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
