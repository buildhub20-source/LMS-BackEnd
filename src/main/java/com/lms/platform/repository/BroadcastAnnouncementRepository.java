package com.lms.platform.repository;

import com.lms.platform.entity.BroadcastAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BroadcastAnnouncementRepository extends JpaRepository<BroadcastAnnouncement, UUID> {

    List<BroadcastAnnouncement> findAllByOrderByCreatedAtDesc();

    @Query("SELECT b FROM BroadcastAnnouncement b WHERE b.active = true AND b.startsAt <= :now AND (b.expiresAt IS NULL OR b.expiresAt > :now) ORDER BY b.startsAt DESC")
    List<BroadcastAnnouncement> findActiveAnnouncements(@Param("now") Instant now);
}
