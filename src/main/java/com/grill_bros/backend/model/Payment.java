package com.grill_bros.backend.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.grill_bros.backend.records.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_order",      columnList = "order_id",    unique = true),
                @Index(name = "idx_payment_status",     columnList = "status"),
                @Index(name = "idx_payment_reference",  columnList = "reference",   unique = true),
                @Index(name = "idx_payment_access_code",columnList = "access_code"),
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @OneToOne(mappedBy = "payment")
    private Receipt receipt;

    @Column(name = "reference", nullable = false, unique = true, length = 100)
    private String reference;

    @Column(name = "access_code", length = 100)
    private String accessCode;

    @Column(name = "authorization_url", length = 500)
    private String authorizationUrl;

    @Column(name = "paystack_transaction_id", length = 100)
    private String paystackTransactionId;

    @Column(name = "customer_email", nullable = false, length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "GHS";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "gateway_response", length = 255)
    private String gatewayResponse;

    @Column(name = "channel", length = 50)
    private String channel;            // card, mobile_money, bank, ussd

    @Column(name = "paid_at")
    private Instant paidAt;

    @OneToMany(
            mappedBy = "payment",
            cascade = CascadeType.ALL,
            orphanRemoval = false
    )
    private Set<PaymentEvent> events = new HashSet<>();

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static Payment create(Order order, String reference,
                                 String customerEmail, String customerPhone,
                                 BigDecimal amount) {
        Payment p         = new Payment();
        p.order        = order;
        p.reference       = reference;
        p.customerEmail   = customerEmail;
        p.customerPhone   = customerPhone;
        p.amount          = amount;
        p.status          = PaymentStatus.INITIATED;
        p.initiatedAt     = Instant.now();
        return p;
    }

    public void transitionTo(PaymentStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Payment cannot transition from " + status + " to " + next);
        }
        this.status = next;
        if (next.isTerminal()) this.completedAt = Instant.now();
    }

    public void markSuccessful(String paystackTxId, String gatewayResponse,
                               String channel, Instant paidAt) {
        transitionTo(PaymentStatus.SUCCESSFUL);
        this.paystackTransactionId = paystackTxId;
        this.gatewayResponse       = gatewayResponse;
        this.channel               = channel;
        this.paidAt                = paidAt;
    }

    public void markFailed(String gatewayResponse) {
        transitionTo(PaymentStatus.FAILED);
        this.gatewayResponse = gatewayResponse;
    }
}
