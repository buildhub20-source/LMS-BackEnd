package com.lms.platform.provisioning;

import com.fasterxml.jackson.databind.JsonNode;
import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.config.PlatformConfig;
import com.lms.platform.entity.Tenant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/** Supabase Management API adapter. Its access token is server-side only. */
@Component
public class SupabaseTenantDatabaseProvisioner implements TenantDatabaseProvisioner {
    private static final String API = "https://api.supabase.com/v1";
    private final PlatformConfig config;
    private final RestClient restClient = RestClient.create();

    public SupabaseTenantDatabaseProvisioner(PlatformConfig config) {
        this.config = config;
    }

    @Override
    public ProvisionedDatabase create(Tenant tenant, String databasePassword) {
        requireConfigured();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", tenant.getName());
        request.put("organization_slug", config.getProvisioning().getSupabase().getOrganizationSlug());
        request.put("db_pass", databasePassword);
        request.put("region", config.getProvisioning().getSupabase().getRegion());
        String size = config.getProvisioning().getSupabase().getInstanceSize();
        if (size != null && !size.isBlank()) request.put("desired_instance_size", size);

        JsonNode response = restClient.post().uri(API + "/projects")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getProvisioning().getSupabase().getAccessToken())
                .contentType(MediaType.APPLICATION_JSON).body(request).retrieve().body(JsonNode.class);
        if (response == null || response.path("ref").asText().isBlank()) {
            throw new ApplicationException(ErrorCode.INTERNAL_ERROR, "Supabase did not return a tenant project reference");
        }
        String ref = response.path("ref").asText();
        return new ProvisionedDatabase(ref,
                "jdbc:postgresql://db." + ref + ".supabase.co:5432/postgres?sslmode=require", "postgres");
    }

    @Override
    public ProvisioningState state(String providerProjectRef) {
        requireConfigured();
        JsonNode response = restClient.get().uri(API + "/projects/{ref}", providerProjectRef)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getProvisioning().getSupabase().getAccessToken())
                .retrieve().body(JsonNode.class);
        String status = response == null ? "" : response.path("status").asText("").toUpperCase();
        // Supabase currently reports fully restored projects as ACTIVE_HEALTHY.
        // Do not use contains("ACTIVE") here: it would also match INACTIVE.
        if (status.startsWith("ACTIVE") || "HEALTHY".equals(status)) return ProvisioningState.READY;
        if ("INACTIVE".equals(status) || status.contains("PAUSED")) return ProvisioningState.PAUSED;
        if (status.contains("FAILED") || status.contains("ERROR")) return ProvisioningState.FAILED;
        return ProvisioningState.PENDING;
    }

    @Override
    public void pause(String providerProjectRef) {
        lifecycleRequest(providerProjectRef, "pause");
    }

    @Override
    public void restore(String providerProjectRef) {
        lifecycleRequest(providerProjectRef, "restore");
    }

    private void lifecycleRequest(String providerProjectRef, String action) {
        requireConfigured();
        if (providerProjectRef == null || providerProjectRef.isBlank()) {
            throw new ApplicationException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "This tenant does not have a cloud project to manage");
        }
        restClient.post().uri(API + "/projects/{ref}/" + action, providerProjectRef)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getProvisioning().getSupabase().getAccessToken())
                .retrieve().toBodilessEntity();
    }

    private void requireConfigured() {
        if (!config.isEnabled()
                || config.getProvisioning().getSupabase().getAccessToken() == null
                || config.getProvisioning().getSupabase().getAccessToken().isBlank()
                || config.getProvisioning().getSupabase().getOrganizationSlug() == null
                || config.getProvisioning().getSupabase().getOrganizationSlug().isBlank()) {
            throw new ApplicationException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Tenant provisioning requires PLATFORM_ENABLED, SUPABASE_ACCESS_TOKEN, and SUPABASE_ORGANIZATION_SLUG");
        }
    }
}
