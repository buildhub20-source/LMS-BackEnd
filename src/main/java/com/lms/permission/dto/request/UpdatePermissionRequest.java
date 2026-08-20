package com.lms.permission.dto.request;

import jakarta.validation.constraints.Size;

public class UpdatePermissionRequest {

    @Size(max = 255)
    private String description;

    public UpdatePermissionRequest() {
    }

    public UpdatePermissionRequest(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
