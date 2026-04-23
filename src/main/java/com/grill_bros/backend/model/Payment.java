package com.grill_bros.backend.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.grill_bros.backend.records.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_order",    columnList = "order_id",   unique = true),
                @Index(name = "idx_payment_status",   columnList = "status"),
                @Index(name = "idx_payment_external", columnList = "external_id", unique = true),
        }
)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Payment extends BaseEntity {

    @Id
    @Column(length = 26, nullable = false, updatable = false)
    private String id;

    @PrePersist
    public void generateId() {
        if (id == null) {
            id = UlidCreator.getUlid().toString();
        }
    }

    @EqualsAndHashCode.Include
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider = "MTN_MOMO";

    @Column(name = "momo_reference", length = 255)
    private String momoReference;

    /** UUID we send to MoMo as our external reference — used for idempotency */
    @Column(name = "external_id", nullable = false, unique = true, length = 255)
    private String externalId;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "GHS";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static Payment create(Order order, String externalId,
                                 String phoneNumber, BigDecimal amount) {
        Payment p     = new Payment();
        p.order       = order;
        p.externalId  = externalId;
        p.phoneNumber = phoneNumber;
        p.amount      = amount;
        p.status      = PaymentStatus.INITIATED;
        p.initiatedAt = Instant.now();
        return p;
    }

    public void transitionTo(PaymentStatus next, String failureReason) {
        if (!this.status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Payment cannot transition from " + status + " to " + next);
        }
        this.status = next;
        if (next.isTerminal()) {
            this.completedAt = Instant.now();
        }
        if (failureReason != null) {
            this.failureReason = failureReason;
        }
    }
}
