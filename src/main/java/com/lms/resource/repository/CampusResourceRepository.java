package com.lms.resource.repository;

import com.lms.resource.entity.CampusResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CampusResourceRepository extends JpaRepository<CampusResource, UUID> {

    List<CampusResource> findAllByOrderByCreatedAtDesc();

    List<CampusResource> findByStatusOrderByCreatedAtDesc(String status);

    List<CampusResource> findByCategoryAndStatusOrderByCreatedAtDesc(String category, String status);

    List<CampusResource> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    @Query("SELECT r FROM CampusResource r WHERE " +
           "(cast(:status as String) IS NULL OR r.status = :status) AND " +
           "(cast(:category as String) IS NULL OR r.category = :category) AND " +
           "(cast(:search as String) IS NULL OR " +
           " LOWER(r.title) LIKE LOWER(CONCAT('%', cast(:search as String), '%')) OR " +
           " LOWER(r.description) LIKE LOWER(CONCAT('%', cast(:search as String), '%')) OR " +
           " LOWER(r.authorName) LIKE LOWER(CONCAT('%', cast(:search as String), '%'))) " +
           "ORDER BY r.createdAt DESC")
    List<CampusResource> searchResources(
            @Param("category") String category,
            @Param("status") String status,
            @Param("search") String search
    );
}
