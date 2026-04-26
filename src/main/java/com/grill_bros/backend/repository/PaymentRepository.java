package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    boolean existsByOrderId(String orderId);

    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(String orderId);

    /**
     * Reconciliation job: find PENDING payments older than a threshold
     */
    @Query("""
            SELECT p FROM Payment p
            WHERE  p.status      = com.grill_bros.backend.records.PaymentStatus.PENDING
              AND  p.initiatedAt < :threshold
            """)
    List<Payment> findStalePayments(@Param("threshold") Instant threshold);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.order.id = :orderId
            ORDER BY p.createdAt DESC
            """)
    List<Payment> findByOrderOrderByCreatedAtDesc(UUID orderId);

    /**
     * Fetch payment with its order in one query for webhook processing
     */
    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.order
            WHERE p.externalId = :externalId
            """)
    Optional<Payment> findByExternalIdWithOrder(@Param("externalId") String externalId);
}
