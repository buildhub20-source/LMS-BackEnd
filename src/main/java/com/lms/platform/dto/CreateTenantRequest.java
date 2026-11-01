package com.lms.platform.dto;

import com.lms.platform.runtime.TenantSlug;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Input accepted only from a global platform administrator. */
public record CreateTenantRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank
        @Size(min = TenantSlug.MIN_LENGTH, max = TenantSlug.MAX_LENGTH)
        @Pattern(regexp = TenantSlug.SHAPE,
                message = "must be lowercase letters, numbers and single hyphens, "
                        + "starting and ending with a letter or number") String slug,
        @NotBlank @Size(max = 100) String ownerName,
        @NotBlank @Email @Size(max = 255) String ownerEmail,
        @NotBlank @Size(min = 12, max = 128) String initialAdminPassword,
        String liveSessionProvider
) {}
