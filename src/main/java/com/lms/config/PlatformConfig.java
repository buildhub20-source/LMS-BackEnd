package com.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the global tenant control plane. */
@Getter
@Setter
@ConfigurationProperties(prefix = "lms.platform")
public class PlatformConfig {
    private boolean enabled;
    private boolean tenantResolutionRequired;
    private String tenantHeader = "X-Tenant-Slug";
    private String baseDomain;
    private String credentialEncryptionKey;
    private GlobalAdmin globalAdmin = new GlobalAdmin();
    private Provisioning provisioning = new Provisioning();
    private RoleMigration roleMigration = new RoleMigration();

    @Getter @Setter
    public static class GlobalAdmin {
        private boolean enabled;
        private String email;
        private String password;
        private String name = "Platform Administrator";
    }

    @Getter @Setter
    public static class Provisioning {
        private String provider = "SUPABASE";
        private Supabase supabase = new Supabase();
    }

    @Getter @Setter
    public static class Supabase {
        private String accessToken;
        private String organizationSlug;
        private String region = "ap-southeast-1";
        private String instanceSize;
    }

    /**
     * Guarded, one-time helper used when moving the control plane to another
     * existing cloud project. It is deliberately disabled by default.
     */
    @Getter @Setter
    public static class RoleMigration {
        private boolean enabled;
        private String sourceTenantSlug;
        private String legacyTenantSlug = "lms";
    }
}
