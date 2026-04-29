package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, String> {

    Optional<Receipt> findByReference(String reference);

    Optional<Receipt> findByPaymentId(String paymentId);
}
