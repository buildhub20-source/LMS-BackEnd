package com.lms.permission.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreatePermissionRequest {

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Permission names must be UPPER_SNAKE_CASE")
    private String name;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Resource must be UPPER_SNAKE_CASE")
    private String resource;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Action must be UPPER_SNAKE_CASE")
    private String action;

    @Size(max = 255)
    private String description;

    public CreatePermissionRequest() {
    }

    public CreatePermissionRequest(String name, String resource, String action, String description) {
        this.name = name;
        this.resource = resource;
        this.action = action;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
