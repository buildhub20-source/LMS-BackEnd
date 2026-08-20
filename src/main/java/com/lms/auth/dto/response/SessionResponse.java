package com.lms.auth.dto.response;

import java.time.Instant;
import java.util.UUID;

/** A live session. Never exposes the refresh token or its digest. */
public class SessionResponse {

    private UUID id;

    private String ipAddress;

    private String userAgent;

    private Instant createdAt;

    private Instant lastUsedAt;

    private Instant expiresAt;

    private boolean current;

    public SessionResponse() {
    }

    public SessionResponse(UUID id, String ipAddress, String userAgent, Instant createdAt, Instant lastUsedAt, Instant expiresAt, boolean current) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
        this.expiresAt = expiresAt;
        this.current = current;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }
}
