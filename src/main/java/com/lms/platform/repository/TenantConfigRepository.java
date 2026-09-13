package com.lms.platform.repository;

import com.lms.platform.entity.TenantConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantConfigRepository extends JpaRepository<TenantConfig, UUID> {
    Optional<TenantConfig> findByTenantId(UUID tenantId);
    boolean existsByTenantId(UUID tenantId);
}
