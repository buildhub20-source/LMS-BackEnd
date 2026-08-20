package com.lms.auth.repository;

import com.lms.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    @Query("""
            select s from UserSession s
            join fetch s.user u
            left join fetch u.userRoles ur
            left join fetch ur.role r
            left join fetch r.permissions
            where s.refreshTokenHash = :tokenHash
            """)
    Optional<UserSession> findByRefreshTokenHash(@Param("tokenHash") String tokenHash);

    List<UserSession> findAllByUserIdAndRevokedFalseOrderByLastUsedAtDesc(UUID userId);

    long countByUserIdAndRevokedFalse(UUID userId);

    @Modifying
    @Query("update UserSession s set s.revoked = true where s.user.id = :userId and s.revoked = false")
    int revokeAllForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from UserSession s where s.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
