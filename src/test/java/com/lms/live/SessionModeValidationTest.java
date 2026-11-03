package com.lms.live;

import com.lms.live.dto.CreateLiveSessionRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SessionModeValidationTest {
    @Test void acceptsOnlyAnExplicitSupportedMode() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var start = Instant.now();
            assertTrue(validator.validate(new CreateLiveSessionRequest("Class", null, start, start.plusSeconds(3600), "live")).isEmpty());
            assertTrue(validator.validate(new CreateLiveSessionRequest("Class", null, start, start.plusSeconds(3600), "google_meet")).isEmpty());
            assertFalse(validator.validate(new CreateLiveSessionRequest("Class", null, start, start.plusSeconds(3600), null)).isEmpty());
            assertFalse(validator.validate(new CreateLiveSessionRequest("Class", null, start, start.plusSeconds(3600), "other")).isEmpty());
        }
    }
}
