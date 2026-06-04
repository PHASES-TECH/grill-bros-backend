package com.grill_bros.backend.service.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grill_bros.backend.dto.paymentdto.InitiatePaymentRequest;
import com.grill_bros.backend.dto.paymentdto.PaymentInitiatedResponse;
import com.grill_bros.backend.dto.paymentdto.PaystackApiResponse;
import com.grill_bros.backend.exceptions.DuplicateResourceException;
import com.grill_bros.backend.exceptions.InvalidStateTransitionException;
import com.grill_bros.backend.exceptions.PaystackException;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.exceptions.passwordexceptions.BadRequestException;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.records.OrderStatus;
import com.grill_bros.backend.records.PaymentStatus;
import com.grill_bros.backend.repository.OrderRepository;
import com.grill_bros.backend.repository.PaymentRepository;
import com.grill_bros.backend.service.eventlisteners.PaymentEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaystackClient paystackClient;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldThrowWhenOrderDoesNotExist() {

        var request = buildRequest();

        when(orderRepository.findById("123"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.initiatePayment(request)
        );
    }

    @Test
    void shouldRejectNonPendingOrder() {

        var request = buildRequest();

        Order order = new Order();
        order.setStatus(OrderStatus.COMPLETED);

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(order));

        assertThrows(
                InvalidStateTransitionException.class,
                () -> paymentService.initiatePayment(request)
        );
    }

    @Test
    void shouldInitializePaymentSuccessfully() {

        Order order = buildOrder();
        var request = buildRequest();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(order));

        when(paymentRepository.findByOrder_Id(any()))
                .thenReturn(Optional.empty());

        when(paystackClient.initializeTransaction(
                anyString(),
                anyString(),
                any()))
                .thenReturn(buildInitResponse());

        when(objectMapper.valueToTree(any()))
                .thenReturn(null);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment p = invocation.getArgument(0);
                    p.setId("test-id");
                    return p;
                });

        PaymentInitiatedResponse response =
                paymentService.initiatePayment(request);

        assertNotNull(response);

        verify(paymentRepository, times(2))
                .save(any(Payment.class));

        verify(paystackClient)
                .initializeTransaction(
                        anyString(),
                        anyString(),
                        any());

        verify(eventPublisher)
                .paymentInitiated(any());
    }

    @Test
    void shouldRejectCompletedPayment() {

        Payment payment = new Payment();
        var request = buildRequest();
        payment.setStatus(PaymentStatus.SUCCESSFUL);

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(buildOrder()));

        when(paymentRepository.findByOrder_Id(any()))
                .thenReturn(Optional.of(payment));

        assertThrows(
                DuplicateResourceException.class,
                () -> paymentService.initiatePayment(request)
        );
    }

    @Test
    void shouldReturnExistingPendingPayment() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);
        payment.setOrder(buildOrder());
        payment.setReference("REF-123");

        var request = buildRequest();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(buildOrder()));

        when(paymentRepository.findByOrder_Id(any()))
                .thenReturn(Optional.of(payment));

        PaymentInitiatedResponse response =
                paymentService.initiatePayment(request);

        verify(paystackClient, never())
                .initializeTransaction(any(), any(), any());

        verify(paymentRepository, times(0))
                .save(any(Payment.class));

        assertNotNull(response);
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    void shouldRequireEmail() {

        Order order = buildOrder();

        var request = buildRequest();
        request.setEmail(null);

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(order));

        assertThrows(
                BadRequestException.class,
                () -> paymentService.initiatePayment(request)
        );
    }

    @Test
    void shouldMarkPaymentFailedWhenPaystackFails() {

        Order order = buildOrder();
        var request = buildRequest();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(order));

        when(paymentRepository.findByOrder_Id(any()))
                .thenReturn(Optional.empty());

        when(paystackClient.initializeTransaction(
                anyString(),
                anyString(),
                any()))
                .thenThrow(
                        new PaystackException("Gateway down")
                );

        assertThrows(
                BadRequestException.class,
                () -> paymentService.initiatePayment(request)
        );

        verify(paymentRepository, atLeastOnce())
                .save(any(Payment.class));
    }

    @Test
    void shouldThrowWhenPaymentNotFound() {

        when(paymentRepository.findByReference(any()))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.verifyAndUpdate("ref")
        );
    }

    @Test
    void shouldSkipVerificationForTerminalPayments() {

        Payment payment = new Payment();
        payment.setOrder(buildOrder());
        payment.setStatus(PaymentStatus.SUCCESSFUL);

        when(paymentRepository.findByReference(any()))
                .thenReturn(Optional.of(payment));

        paymentService.verifyAndUpdate("ref");

        verify(paystackClient, never())
                .verifyTransaction(any());
    }

    @Test
    void shouldHandleSuccessfulPaymentAndConfirmOrder() {

        Order order = buildOrder();
        order.setId("order-1");
        order.setStatus(OrderStatus.PENDING);

        Payment payment = new Payment();
        payment.setId("pay-1");
        payment.setOrder(order);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setReference("ref-123");

        PaystackApiResponse.VerifyData verifyData =
                mock(PaystackApiResponse.VerifyData.class);

        when(verifyData.getStatus()).thenReturn("success");
        when(verifyData.getId()).thenReturn(1001L);
        when(verifyData.getGatewayResponse()).thenReturn("Approved");
        when(verifyData.getChannel()).thenReturn("card");
        when(verifyData.getPaidAt()).thenReturn(Instant.now());

        when(paymentRepository.findByReference("ref-123"))
                .thenReturn(Optional.of(payment));

        when(orderRepository.findById("order-1"))
                .thenReturn(Optional.of(order));

        when(paystackClient.verifyTransaction("ref-123"))
                .thenReturn(verifyData);

        var response = paymentService.verifyAndUpdate("ref-123");

        assertNotNull(response);

        verify(paymentRepository).save(payment);
        verify(orderRepository).save(order);
        verify(eventPublisher).paymentSucceeded(payment);

        assertEquals(PaymentStatus.SUCCESSFUL, payment.getStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void shouldHandleFailedPaymentAndMarkOrderFailed() {

        Order order = buildOrder();
        order.setId("order-1");
        order.setStatus(OrderStatus.PENDING);

        Payment payment = new Payment();
        payment.setId("pay-1");
        payment.setReference("ref-123");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setOrder(order);

        PaystackApiResponse.VerifyData verifyData =
                mock(PaystackApiResponse.VerifyData.class);

        when(verifyData.getGatewayResponse())
                .thenReturn("Insufficient funds");

        when(paymentRepository.findByReference("ref-123"))
                .thenReturn(Optional.of(payment));

        when(orderRepository.findById("order-1"))
                .thenReturn(Optional.of(order));

        when(paystackClient.verifyTransaction("ref-123"))
                .thenReturn(verifyData);

        when(verifyData.getStatus()).thenReturn("failed");

        paymentService.verifyAndUpdate("ref-123");

        assertEquals(PaymentStatus.FAILED, payment.getStatus());

        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());

        verify(paymentRepository).save(payment);
        verify(orderRepository).save(order);

        verify(eventPublisher).paymentFailed(payment);
    }

    @Test
    void shouldNotFailWhenOrderAlreadyTransitioned() {

        Order order = buildOrder();
        order.setId("order-1");
        order.setStatus(OrderStatus.CONFIRMED);

        Payment payment = new Payment();
        payment.setReference("ref-123");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setOrder(order);

        PaystackApiResponse.VerifyData verifyData =
                mock(PaystackApiResponse.VerifyData.class);

        when(verifyData.getGatewayResponse())
                .thenReturn("Payment failed");

        when(paymentRepository.findByReference("ref-123"))
                .thenReturn(Optional.of(payment));

        when(orderRepository.findById("order-1"))
                .thenReturn(Optional.of(order));

        when(paystackClient.verifyTransaction("ref-123"))
                .thenReturn(verifyData);

        when(verifyData.getStatus()).thenReturn("failed");

        paymentService.verifyAndUpdate("ref-123");

        assertEquals(PaymentStatus.FAILED, payment.getStatus());

        verify(orderRepository).save(order);
    }

    private PaystackApiResponse.InitializeData buildInitResponse() {

        PaystackApiResponse.InitializeData data =
                new PaystackApiResponse.InitializeData();

        data.setAuthorizationUrl("https://paystack.com/pay/mock-auth-url");
        data.setAccessCode("mock-access-code");
        data.setReference("REF-123456");

        return data;
    }

    private InitiatePaymentRequest buildRequest() {
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setOrderId("123");
        request.setEmail("customer@test.com");
        return request;
    }

    private Order buildOrder() {
        Order order = new Order();
        order.setId("123");
        order.setOrderNumber("ORD-001");
        order.setStatus(OrderStatus.PENDING);
        order.setCustomerEmail("customer@test.com");
        order.setTotalAmount(BigDecimal.valueOf(100));
        return order;
    }
}
