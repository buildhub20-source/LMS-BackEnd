package com.lms.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Input accepted only from a global platform administrator. */
public record CreateTenantRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$",
                message = "must be 3-63 lowercase letters, numbers, or hyphens") String slug,
        @NotBlank @Size(max = 100) String ownerName,
        @NotBlank @Email @Size(max = 255) String ownerEmail,
        @NotBlank @Size(min = 12, max = 128) String initialAdminPassword
) {}
