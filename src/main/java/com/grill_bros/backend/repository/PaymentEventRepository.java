package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, String> {

    List<PaymentEvent> findAllByPaymentIdOrderByCreatedAtAsc(String paymentId);
}
