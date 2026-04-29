package com.grill_bros.backend.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.grill_bros.backend.records.ReceiptStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payment_receipts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Receipt extends BaseEntity{

    @Id
    @Column(length = 26, nullable = false, updatable = false)
    private String id;

    @PrePersist
    public void generateId() {
        if (id == null) {
            id = UlidCreator.getUlid().toString();
        }
    }

    @Column(unique = true, nullable = false)
    private String reference; // e.g. RCPT-8F3K92LQ

    private BigDecimal amount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    private String currency;

    private String orderId;

    private String customerName;

    private String customerEmail;

    private String customerPhone;

    private String pdfUrl; // if stored in S3/local

    @Enumerated(EnumType.STRING)
    private ReceiptStatus status;

    private Instant issuedAt;
}
