package com.lms.live.repository;

import com.lms.live.entity.LiveSessionReminder;
import com.lms.live.entity.LiveSessionReminderKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LiveSessionReminderRepository extends JpaRepository<LiveSessionReminder, UUID> {

    boolean existsBySessionIdAndKind(UUID sessionId, LiveSessionReminderKind kind);
}
