package com.lms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class UnlockAccountTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void unlockAllAccounts() {
        int updated = jdbcTemplate.update("UPDATE lms.users SET is_locked = false");
        System.out.println("=========================================");
        System.out.println("Unlocked accounts: " + updated);
        System.out.println("=========================================");
    }
}
