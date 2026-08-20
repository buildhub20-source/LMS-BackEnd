package com.lms.auth.repository;

import com.lms.auth.entity.LoginAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    /** Timestamp of the most recent successful login, or null if there is none. */
    @Query("select max(a.attemptedAt) from LoginAttempt a where lower(a.email) = lower(:email) and a.success = true")
    Instant findLastSuccessAt(@Param("email") String email);

    @Query("""
            select count(a) from LoginAttempt a
            where lower(a.email) = lower(:email)
              and a.success = false
              and a.attemptedAt > :after
            """)
    long countFailuresAfter(@Param("email") String email, @Param("after") Instant after);

    Page<LoginAttempt> findAllByEmailIgnoreCaseOrderByAttemptedAtDesc(String email, Pageable pageable);

    Page<LoginAttempt> findAllByUserIdOrderByAttemptedAtDesc(UUID userId, Pageable pageable);
}
