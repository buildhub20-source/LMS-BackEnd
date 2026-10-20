package com.lms.announcement.repository;

import com.lms.announcement.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    List<Announcement> findAllByOrderByCreatedAtDesc();

    List<Announcement> findAllByCreatedByOrderByCreatedAtDesc(UUID createdBy);

    @Query("""
            select a from Announcement a
            where a.active = true and (a.expiresAt is null or a.expiresAt > :now)
            order by a.createdAt desc
            """)
    List<Announcement> findVisible(@Param("now") Instant now);
}
