package com.lms.organization.entity;

import com.lms.common.audit.Timestamped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Platform-wide organisation settings — logo, branding, contact details.
 * A single row is expected; use {@code findFirstBy()} to retrieve it.
 */
@Entity
@Table(name = "organization_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSettings extends Timestamped {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 255)
    private String domain;

    @Column(name = "support_email", length = 255)
    private String supportEmail;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "primary_color", length = 16)
    private String primaryColor;

    @Column(name = "logo_url", length = 1024)
    private String logoUrl;
}
