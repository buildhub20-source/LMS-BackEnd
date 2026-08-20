package com.lms.role.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class CreateRoleRequest {

    @NotBlank
    @Size(max = 60)
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Role names must be UPPER_SNAKE_CASE")
    private String name;

    @Size(max = 255)
    private String description;

    private Set<String> permissions;

    public CreateRoleRequest() {
    }

    public CreateRoleRequest(String name, String description, Set<String> permissions) {
        this.name = name;
        this.description = description;
        this.permissions = permissions;
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

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
