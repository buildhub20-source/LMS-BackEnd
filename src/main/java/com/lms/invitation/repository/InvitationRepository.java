package com.lms.invitation.repository;

import com.lms.invitation.entity.Invitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    @Query("""
            select i from Invitation i
            join fetch i.user u
            left join fetch u.userRoles ur
            left join fetch ur.role
            where i.tokenHash = :tokenHash
            """)
    Optional<Invitation> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Query(value = """
            select i from Invitation i
            join fetch i.user
            left join fetch i.invitedBy
            """,
            countQuery = "select count(i) from Invitation i")
    Page<Invitation> findAllWithUser(Pageable pageable);

    @Query("""
            select i from Invitation i
            join fetch i.user
            left join fetch i.invitedBy
            where i.id = :id
            """)
    Optional<Invitation> findByIdWithUser(@Param("id") UUID id);

    @Query("""
            select count(i) from Invitation i
            where i.user.id = :userId and i.acceptedAt is null and i.expiresAt > :now
            """)
    long countPendingForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    Optional<Invitation> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * The outstanding invitation for a user, if any. Its presence is what marks
     * the account as still holding a temporary password.
     */
    Optional<Invitation> findFirstByUserIdAndAcceptedAtIsNullOrderByCreatedAtDesc(UUID userId);

    void deleteAllByUserId(UUID userId);
}
