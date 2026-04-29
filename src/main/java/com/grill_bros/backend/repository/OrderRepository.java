package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.records.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    long countByCreatedAtBetween(Instant start, Instant end);

//    @Query("""
//        SELECT o FROM Order o
//        WHERE (:status       IS NULL OR o.status          = :status)
//          AND (:phone        IS NULL OR o.customerPhone   LIKE CONCAT('%',:phone,'%'))
//          AND (:customerName IS NULL OR LOWER(o.customerName) LIKE LOWER(CONCAT('%',:customerName,'%')))
//          AND (:from         IS NULL OR o.createdAt       >= :from)
//          AND (:to           IS NULL OR o.createdAt       <= :to)
//        ORDER BY o.createdAt DESC
//        """)
//    Page<Order> findAllForAdmin(
//            @Param("status") OrderStatus status,
//            @Param("phone")        String phone,
//            @Param("customerName") String customerName,
//            @Param("from")         Instant from,
//            @Param("to")           Instant to,
//            Pageable pageable);

    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.items oi
        LEFT JOIN FETCH oi.menuItem
        WHERE o.id = :id
        """)
    Optional<Order> findByIdWithItems(@Param("id") String id);

    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.items oi
        LEFT JOIN FETCH oi.menuItem
        WHERE o.orderNumber = :orderNumber
        """)
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);

    // ── Revenue / dashboard aggregates ───────────────────────────────────────

    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.status = com.grill_bros.backend.records.OrderStatus.COMPLETED
          AND o.createdAt >= :from
          AND o.createdAt <= :to
        """)
    long countCompletedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0)
        FROM   Order o
        WHERE  o.status = com.grill_bros.backend.records.OrderStatus.COMPLETED
          AND  o.createdAt >= :from
          AND  o.createdAt <= :to
        """)
    BigDecimal sumRevenueBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        SELECT COALESCE(AVG(o.totalAmount), 0)
        FROM   Order o
        WHERE  o.status = com.grill_bros.backend.records.OrderStatus.COMPLETED
          AND  o.createdAt >= :from
          AND  o.createdAt <= :to
        """)
    BigDecimal avgOrderValueBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** Count grouped by status — used for order distribution widget */
    @Query("""
        SELECT o.status, COUNT(o) FROM Order o
        WHERE o.createdAt >= :from
        GROUP BY o.status
        """)
    List<Object[]> countByStatusSince(@Param("from") Instant from);

    /** Daily revenue buckets for revenue chart */
    @Query(value = """
        SELECT DATE(created_at AT TIME ZONE 'UTC') AS day,
               COUNT(*)                             AS order_count,
               COALESCE(SUM(total_amount), 0)       AS revenue
        FROM   orders
        WHERE  status    = 'COMPLETED'
          AND  created_at >= :from
          AND  created_at <= :to
        GROUP  BY day
        ORDER  BY day ASC
        """, nativeQuery = true)
    List<Object[]> dailyRevenueBetween(@Param("from") Instant from, @Param("to") Instant to);

    // ── Phase 2 placeholder ───────────────────────────────────────────────────

    Page<Order> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
