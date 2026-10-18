package com.lms.organization.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.TreeSet;

/**
 * Partial update of the organisation settings: a field left out (null) keeps
 * its value; an empty string clears an optional field.
 */
@Getter
@Setter
public class UpdateOrganizationSettingsRequest {

    @Size(max = 160, message = "must be at most 160 characters")
    @Pattern(regexp = "^(?!\\s*$).+", message = "must not be blank")
    private String name;

    @Size(max = 255, message = "must be at most 255 characters")
    @Email(message = "must be a valid email address")
    private String supportEmail;

    @Size(max = 2000, message = "must be at most 2000 characters")
    private String description;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex colour such as #10b981")
    private String primaryColor;

    @Size(max = 1024, message = "must be at most 1024 characters")
    @Pattern(regexp = "^$|^https://\\S+$", message = "must be an https:// address")
    private String logoUrl;

    @JsonIgnore
    private final Set<String> unknownFields = new TreeSet<>();

    @JsonAnySetter
    void rejectUnknown(String field, Object ignored) {
        unknownFields.add(field);
    }

    @JsonIgnore
    @AssertTrue(message = "is not a recognised setting")
    public boolean isOnlyKnownFields() {
        return unknownFields.isEmpty();
    }
}
