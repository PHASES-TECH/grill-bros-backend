package com.grill_bros.backend.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.grill_bros.backend.records.ReceiptStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "payment_receipts",
        indexes = {
                @Index(name = "idx_receipt_reference", columnList = "reference", unique = true)
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
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

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "payment_id",
            nullable = false,
            unique = true
    )
    private Payment payment;

    private String currency;

    private String customerName;

    private String customerEmail;

    private String customerPhone;

    private String pdfUrl;

    @Enumerated(EnumType.STRING)
    private ReceiptStatus status;

    private Instant issuedAt;
}
