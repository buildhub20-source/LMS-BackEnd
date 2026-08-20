package com.lms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LmsApplicationTests {

    @Test
    void contextLoads() {
        // Fails the build if any bean in the application cannot be wired.
    }
}
