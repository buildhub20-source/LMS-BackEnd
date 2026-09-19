package com.lms.common.dto.internal;

import java.util.List;
import java.util.UUID;

/**
 * Request body for batch user lookups from internal services.
 */
public record InternalUserBatchRequest(
        List<UUID> userIds
) {}
