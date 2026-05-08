package com.grill_bros.backend.service.utilsservice;

import com.grill_bros.backend.dto.paymentdto.PaymentReceiptResponse;
import com.grill_bros.backend.dto.paymentdto.PaymentResponse;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.model.Receipt;
import com.grill_bros.backend.records.ReceiptStatus;
import com.grill_bros.backend.repository.OrderRepository;
import com.grill_bros.backend.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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

    public void adminGenerateAndSendReceipt(String orderId) {

        // 🔒 Prevent duplicate receipts
        if (receiptRepository.findByOrderId(orderId).isPresent()) {
            return;
        }

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // 1. Create receipt
        Receipt receipt = Receipt.builder()
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

    private String generateReference() {
        return "RCPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
