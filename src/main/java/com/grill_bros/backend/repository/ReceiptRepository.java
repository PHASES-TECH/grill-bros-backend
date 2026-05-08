package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, String> {

    Optional<Receipt> findByReference(String reference);

    Optional<Receipt> findByPaymentId(String paymentId);

    Optional<Receipt> findByOrderId(String orderId);
}
