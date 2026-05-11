package com.grill_bros.backend.service.utilsservice;

import com.grill_bros.backend.dto.paymentdto.PaymentReceiptResponse;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.model.Receipt;
import com.grill_bros.backend.records.ReceiptStatus;
import com.grill_bros.backend.repository.OrderRepository;
import com.grill_bros.backend.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final OrderRepository orderRepository;

    public Page<PaymentReceiptResponse> getAllPaymentReceipts(Pageable pageable) {
        return receiptRepository.findAll(pageable).map(PaymentReceiptResponse::summary);
    }

    @Transactional
    public void generateAndSendReceipt(Payment payment) {

        // 🔒 Prevent duplicate receipts
        if (receiptRepository.findByPaymentId(payment.getId()).isPresent()) {
            return;
        }

        // 1. Create receipt
        Receipt receipt = Receipt.builder()
                .reference(generateReference())
                .amount(payment.getAmount())
                .currency("GHS")
                .payment(payment)
                .orderId(payment.getOrder().getId())
                .customerName(payment.getOrder().getCustomerName())
                .customerEmail(payment.getOrder().getCustomerEmail())
                .customerPhone(payment.getOrder().getCustomerPhone())
                .status(ReceiptStatus.GENERATED)
                .issuedAt(Instant.now())
                .build();

        receiptRepository.save(receipt);

        try {
            // 2. Generate PDF
            byte[] pdfBytes = pdfService.generateReceiptPdf(receipt);

            // 3. Send email
            emailService.sendReceiptEmail(
                    receipt.getCustomerEmail(),
                    receipt,
                    pdfBytes
            );

            receipt.setStatus(ReceiptStatus.SENT);

        } catch (Exception e) {
            receipt.setStatus(ReceiptStatus.FAILED);
        }

        receiptRepository.save(receipt);
    }

    @Transactional
    public void adminGenerateAndSendReceipt(String orderId) {

        Receipt receipt = receiptRepository.findByOrderId(orderId)
                .map(existing -> {
                    if (existing.getStatus() == ReceiptStatus.SENT) {
                        return existing;
                    }

                    existing.setStatus(ReceiptStatus.GENERATED);
                    existing.setIssuedAt(Instant.now());

                    return existing;
                })
                .orElseGet(() -> {
                    Order order = orderRepository.findById(orderId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException("Order not found"));

                    return Receipt.builder()
                            .reference(generateReference())
                            .amount(order.getTotalAmount())
                            .currency("GHS")
                            .orderId(order.getId())
                            .customerName(order.getCustomerName())
                            .customerEmail(order.getCustomerEmail())
                            .customerPhone(order.getCustomerPhone())
                            .status(ReceiptStatus.GENERATED)
                            .issuedAt(Instant.now())
                            .build();
                });

        // already sent
        if (receipt.getStatus() == ReceiptStatus.SENT) {
            return;
        }

        try {

            byte[] pdfBytes = pdfService.generateReceiptPdf(receipt);

            emailService.sendReceiptEmail(
                    receipt.getCustomerEmail(),
                    receipt,
                    pdfBytes
            );

            receipt.setStatus(ReceiptStatus.SENT);

        } catch (Exception e) {
            log.error("adminGenerateAndSendReceipt failed", e);
            receipt.setStatus(ReceiptStatus.FAILED);
            return;
        }

        receiptRepository.save(receipt);
    }

    private String generateReference() {
        return "RCPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
