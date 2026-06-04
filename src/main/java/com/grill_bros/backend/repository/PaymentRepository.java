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

    Optional<Payment> findByOrder_Id(String orderId);

    boolean existsByReference(String reference);

    boolean existsByOrder_Id(String orderId);

    Optional<Payment> findByReference(String externalId);

    @Query("""
            SELECT p FROM Payment p
            WHERE  p.status      = com.grill_bros.backend.records.PaymentStatus.PENDING
              AND  p.initiatedAt < :threshold
            """)
    List<Payment> findStalePayments(@Param("threshold") Instant threshold);
}
