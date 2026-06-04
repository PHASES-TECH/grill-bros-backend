package com.grill_bros.backend.service.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.grill_bros.backend.exceptions.PaystackException;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.model.PaymentEvent;
import com.grill_bros.backend.records.PaymentStatus;
import com.grill_bros.backend.repository.PaymentEventRepository;
import com.grill_bros.backend.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentReconciliationJobServiceTest {

    @InjectMocks
    private PaymentReconciliationJob job;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventRepository paymentEventRepository;

    @Mock
    private PaystackClient paystackClient;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void shouldDoNothingWhenNoStalePayments() {

        when(paymentRepository.findStalePayments(any()))
                .thenReturn(List.of());

        job.reconcile();

        verifyNoInteractions(paymentService);
        verifyNoInteractions(paymentEventRepository);
        verifyNoInteractions(paystackClient);
    }

    @Test
    void shouldMarkPaymentAsTimeoutWhenOlderThanThreshold() {

        Payment payment = new Payment();
        payment.setReference("ref-1");
        payment.setInitiatedAt(Instant.now().minus(20, ChronoUnit.MINUTES));
        payment.setStatus(PaymentStatus.INITIATED);

        when(paymentRepository.findStalePayments(any()))
                .thenReturn(List.of(payment));

        ObjectNode node = new ObjectMapper().createObjectNode();
        node.put("message", "test");

        when(objectMapper.createObjectNode()).thenReturn(node);

        job.reconcile();

        assertEquals(PaymentStatus.TIMEOUT, payment.getStatus());

        verify(paymentRepository).save(payment);
        verify(paymentEventRepository).save(any());
        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldVerifyPaymentWhenWithinTimeoutWindow() {

        Payment payment = new Payment();
        payment.setReference("ref-1");
        payment.setInitiatedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findStalePayments(any()))
                .thenReturn(List.of(payment));

        job.reconcile();

        verify(paymentService).verifyAndUpdate("ref-1");
    }

    @Test
    void shouldContinueWhenPaystackFails() {

        Payment payment = new Payment();
        payment.setReference("ref-1");
        payment.setInitiatedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findStalePayments(any()))
                .thenReturn(List.of(payment));

        doThrow(new PaystackException("API down"))
                .when(paymentService)
                .verifyAndUpdate("ref-1");

        job.reconcile();

        verify(paymentService).verifyAndUpdate("ref-1");
        verify(paymentEventRepository, never()).save(any());
    }

}
