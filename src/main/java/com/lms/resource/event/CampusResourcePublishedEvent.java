package com.lms.resource.event;

import java.util.UUID;

/** Raised when a campus toolkit resource is published to students. */
public record CampusResourcePublishedEvent(UUID resourceId) {
}
