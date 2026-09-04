package com.lms.user.repository;

import com.lms.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Loads the aggregate with the full authorization graph initialised.
     * Both fetched collections are Sets, so the multiple-fetch restriction on
     * bags does not apply.
     */
    @Query("""
            select distinct u from User u
            left join fetch u.userRoles ur
            left join fetch ur.role r
            left join fetch r.permissions
            where lower(u.email) = lower(:email)
            """)
    Optional<User> findByEmailWithAuthorities(@Param("email") String email);

    @Query("""
            select distinct u from User u
            left join fetch u.userRoles ur
            left join fetch ur.role r
            left join fetch r.permissions
            where u.id = :id
            """)
    Optional<User> findByIdWithAuthorities(@Param("id") UUID id);

    @Query("""
            select u from User u
            where (cast(:search as String) is null
                   or lower(u.name) like lower(concat('%', cast(:search as String), '%'))
                   or lower(u.email) like lower(concat('%', cast(:search as String), '%')))
              and (:active is null or cast(:active as boolean) is null or u.active = :active)
            """)
    Page<User> search(@Param("search") String search,
                      @Param("active") Boolean active,
                      Pageable pageable);

    @Query("select count(u) from User u join u.userRoles ur where ur.role.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    @Query("select distinct u from User u join u.userRoles ur where ur.role.name = :roleName")
    java.util.List<User> findUsersByRoleName(@Param("roleName") String roleName);
}

