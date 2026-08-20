package com.lms.common.audit;

import java.util.UUID;

/** Records security-relevant events in the audit log. */
public interface AuditService {

    /** Records an event as part of the caller transaction. */
    void record(AuditAction action, String resource, UUID resourceId, String details);

    /** Records an event for the given actor rather than the current principal. */
    void record(UUID actorId, AuditAction action, String resource, UUID resourceId, String details);

    /**
     * Records an event in its own transaction so it survives a rollback of the
     * caller. Used for failure paths such as a rejected login.
     */
    void recordIndependently(UUID actorId, AuditAction action, String resource, UUID resourceId, String details);
}
