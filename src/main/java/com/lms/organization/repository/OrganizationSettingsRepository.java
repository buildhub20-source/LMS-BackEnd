package com.lms.organization.repository;

import com.lms.organization.entity.OrganizationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, UUID> {

    /**
     * Returns the first (and typically only) org-settings row.
     * This LMS installation maintains a single platform-wide configuration.
     */
    Optional<OrganizationSettings> findFirstBy();
}
