package com.lms.common.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Transport representation of an audit log entry enriched with actor information.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private UUID id;

    private UUID userId;

    private String userName;

    private String userEmail;

    private AuditAction action;

    private String resource;

    private UUID resourceId;

    private String details;

    private String ipAddress;

    private String userAgent;

    private Instant createdAt;
}
