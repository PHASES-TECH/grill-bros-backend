package com.grill_bros.backend.service.utilsservice;

import com.grill_bros.backend.dto.paymentdto.PaymentReceiptResponse;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.Payment;
import com.grill_bros.backend.model.Receipt;
import com.grill_bros.backend.records.ReceiptStatus;
import com.grill_bros.backend.repository.OrderRepository;
import com.grill_bros.backend.repository.PaymentRepository;
import com.grill_bros.backend.repository.ReceiptRepository;
import com.grill_bros.backend.service.smsservice.SmsProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final OrderRepository orderRepository;
    private final ReceiptStorageService receiptStorageService;
    private final SmsProviderService smsProviderService;
    private final PaymentRepository paymentRepository;

    public Page<PaymentReceiptResponse> getAllPaymentReceipts(Pageable pageable) {
        return receiptRepository.findAll(pageable).map(PaymentReceiptResponse::summary);
    }

    @Transactional
    public void generateAndSendReceipt(Payment payment) {

        // 🔒 Prevent duplicate receipts
        if (receiptRepository.findByPayment_Id(payment.getId()).isPresent()) {
            return;
        }

        // 1. Create receipt
        Receipt receipt = Receipt.builder()
                .reference(generateReference())
                .amount(payment.getAmount())
                .currency("GHS")
                .payment(payment)
                .customerName(payment.getOrder().getCustomerName())
                .customerEmail(payment.getOrder().getCustomerEmail())
                .customerPhone(payment.getOrder().getCustomerPhone())
                .status(ReceiptStatus.GENERATED)
                .issuedAt(Instant.now())
                .build();

        receiptRepository.save(receipt);

        try {
            byte[] pdfBytes = pdfService.generateReceiptPdf(receipt);

            if (receipt.getCustomerEmail() != null && !receipt.getCustomerEmail().isBlank()) {
                emailService.sendReceiptEmail(
                        receipt.getCustomerEmail(),
                        receipt,
                        pdfBytes
                );
            } else {
                String receiptUrl =
                        receiptStorageService.uploadReceiptPdf(
                                pdfBytes,
                                receipt.getReference()
                        );

                String message = String.format(
                        "Thank you for your order. View your receipt here: %s",
                        receiptUrl
                );

                smsProviderService.sendSms(
                        List.of(receipt.getCustomerPhone()),
                        receiptUrl
                );
            }

            receipt.setStatus(ReceiptStatus.SENT);

        } catch (Exception e) {
            log.error("adminGenerateAndSendReceipt failed", e);
            receipt.setStatus(ReceiptStatus.FAILED);
            return;
        }

        receiptRepository.save(receipt);
    }

    @Transactional
    public void adminGenerateAndSendReceipt(String paymentId) {

        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException("Payment"));

        Receipt receipt = receiptRepository.findByPayment_Id(paymentId)
                .map(existing -> {
                    if (existing.getStatus() == ReceiptStatus.SENT) {
                        return existing;
                    }

                    existing.setStatus(ReceiptStatus.GENERATED);
                    existing.setIssuedAt(Instant.now());

                    return existing;
                })
                .orElseGet(() -> {
                    Order order = orderRepository.findById(payment.getOrder().getId())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException("Order not found"));

                    return Receipt.builder()
                            .reference(generateReference())
                            .amount(order.getTotalAmount())
                            .currency("GHS")
                            .customerName(order.getCustomerName())
                            .customerEmail(order.getCustomerEmail())
                            .customerPhone(order.getCustomerPhone())
                            .status(ReceiptStatus.GENERATED)
                            .issuedAt(Instant.now())
                            .build();
                });

        if (receipt.getStatus() == ReceiptStatus.SENT) {
//           String receiptUrl = receipt.getPdfUrl();
//
//            String message = String.format(
//                    "Thank you for your order. View your receipt here: %s",
//                    receiptUrl
//            );
//
//            smsProviderService.sendSms(
//                    List.of(receipt.getCustomerPhone()),
//                    message
//            );
            return;
        }

        try {
            byte[] pdfBytes = pdfService.generateReceiptPdf(receipt);

            if (receipt.getCustomerEmail() != null && !receipt.getCustomerEmail().isBlank()) {
                String receiptUrl = receiptStorageService.uploadReceiptPdf(
                        pdfBytes,
                        receipt.getReference()
                );

                receipt.setPdfUrl(receiptUrl);

                emailService.sendReceiptEmail(
                        receipt.getCustomerEmail(),
                        receipt,
                        pdfBytes
                );
            } else {
                String receiptUrl;
                if (receipt.getPdfUrl() != null && !receipt.getPdfUrl().isBlank()) {
                    log.info("Reusing existing receipt URL for reference {}",
                            receipt.getReference());
                    receiptUrl = receipt.getPdfUrl();
                } else {
                    receiptUrl = receiptStorageService.uploadReceiptPdf(
                            pdfBytes,
                            receipt.getReference()
                    );

                    receipt.setPdfUrl(receiptUrl);
                }

                String message = String.format(
                        "Thank you for your order. View your receipt here: %s",
                        receiptUrl
                );

                smsProviderService.sendSms(
                        List.of(receipt.getCustomerPhone()),
                        message
                );
            }

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
