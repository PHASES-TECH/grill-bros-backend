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

    // ── Public menu queries ─────────────────────────────────────────────────

    /** Paginated list of available, active items — used by GET /menu/items */
    Page<MenuItem> findAllByActiveTrueAndAvailableTrue(Pageable pageable);

    /**
     * All available items in a category ordered by sortOrder.
     * Returned as List (not Page) because the category /items endpoint
     * returns the full set — categories are small (<50 items typical).
     * Result cached in Redis under quickbite:menu:category:{slug}.
     */
    List<MenuItem> findAllByCategoryAndActiveTrueAndAvailableTrueOrderBySortOrderAsc(
            MenuCategory category);

    /** Single item detail — public. Returns empty if soft-deleted. */
    Optional<MenuItem> findByIdAndActiveTrue(UUID id);

    /**
     * ILIKE full-text search across name and description.
     * LOWER(CONCAT('%',:query,'%')) — safe against SQL injection via
     * JPA parameterised binding; no native query needed.
     */
    @Query("""
        SELECT m FROM MenuItem m
        WHERE m.active = true
          AND m.available = true
          AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY m.sortOrder ASC
        """)
    Page<MenuItem> searchAvailable(@Param("query") String query, Pageable pageable);

    // ── Admin queries (include unavailable items) ────────────────────────────

    /**
     * Admin item list with optional category and availability filters.
     * :categoryId IS NULL is evaluated by JPA — passes null safely.
     * :available IS NULL allows the admin to see all items regardless of stock.
     */
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

    /**
     * Top N selling items by quantity in COMPLETED orders since :from.
     *
     * Returns Object[] rows — mapped to TopItemResponse in DashboardService:
     *   [0]  UUID        oi.menuItem.id     (nullable — item may be deleted)
     *   [1]  String      oi.itemName        (denormalised snapshot)
     *   [2]  Long        SUM(oi.quantity)   aliased totalQty
     *   [3]  BigDecimal  SUM(oi.lineTotal)  aliased totalRevenue
     *
     * Pageable is used only for LIMIT via PageRequest.of(0, limit).
     * Relies on idx_order_item_menuitem + idx_order_created composite scan.
     */
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

    // ── Bulk operations ──────────────────────────────────────────────────────

    /**
     * Bulk-toggle availability for an entire category.
     * Used when a category is deactivated (e.g. kitchen closes a section).
     * Returns number of rows updated for logging.
     * @Modifying required for UPDATE queries — triggers cache eviction in MenuService.
     */
    @Modifying
    @Query("UPDATE MenuItem m SET m.available = :available WHERE m.category.id = :catId")
    int bulkSetAvailabilityByCategory(
            @Param("catId")     UUID    catId,
            @Param("available") boolean available);
}
