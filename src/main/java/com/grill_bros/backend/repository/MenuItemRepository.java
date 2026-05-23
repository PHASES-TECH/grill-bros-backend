package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.MenuCategory;
import com.grill_bros.backend.model.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    Page<MenuItem> findAllByActiveTrueAndAvailableTrue(Pageable pageable);

//    @Query(
//            value = "SELECT m FROM menuItem m WHERE m.active = true AND m.available = true",
//            countQuery = "SELECT count(m) FROM menuItem m WHERE m.active = true AND m.available = true"
//    )
//    Page<MenuItem> findAllActiveAvailable(Pageable pageable);

    Page<MenuItem> findAllByCategoryAndActiveTrueAndAvailableTrue(
            MenuCategory category,
            Pageable pageable
    );

    Optional<MenuItem> findByIdAndActiveTrue(UUID id);

    @Query("""
        SELECT m FROM MenuItem m
        WHERE m.active = true
          AND m.available = true
          AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY m.sortOrder ASC
        """)
    Page<MenuItem> searchAvailable(@Param("query") String query, Pageable pageable);

    @Query("""
        SELECT m FROM MenuItem m
        WHERE m.active = true
          AND (:categoryId IS NULL OR m.category.id = :categoryId)
          AND (:available  IS NULL OR m.available   = :available)
        """)
    Page<MenuItem> findAllForAdmin(
            @Param("categoryId") UUID    categoryId,
            @Param("available")  Boolean available,
            Pageable pageable);

    // ── Analytics ────────────────────────────────────────────────────────────

    @Query("""
        SELECT oi.menuItem.id, oi.itemName,
               SUM(oi.quantity)  AS totalQty,
               SUM(oi.lineTotal) AS totalRevenue
        FROM   OrderItem oi
        JOIN   oi.order o
        WHERE  o.status   = com.grill_bros.backend.records.OrderStatus.COMPLETED
          AND  o.createdAt >= :from
        GROUP BY oi.menuItem.id, oi.itemName
        ORDER BY totalQty DESC
        """)
    List<Object[]> findTopItemsRaw(@Param("from") Instant from, Pageable pageable);

    @Modifying
    @Query("UPDATE MenuItem m SET m.available = :available WHERE m.category.id = :catId")
    int bulkSetAvailabilityByCategory(
            @Param("catId")     UUID    catId,
            @Param("available") boolean available);
}
