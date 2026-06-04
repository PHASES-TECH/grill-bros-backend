package com.grill_bros.backend.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.grill_bros.backend.records.PaymentEventType;
import com.grill_bros.backend.records.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable append-only audit log for every payment state change or webhook.
 * Never deleted. Provides full traceability for financial reconciliation.
 */
@Entity
@Table(
        name = "payment_events",
        indexes = {
                @Index(name = "idx_pay_event_payment", columnList = "payment_id"),
                @Index(name = "idx_pay_event_type",    columnList = "event_type"),
                @Index(name = "idx_pay_event_created", columnList = "created_at"),
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "event_type", nullable = false, length = 100)
    private PaymentEventType eventType;

    @Column(name = "old_status", length = 50)
    private String oldStatus;

    @Column(name = "new_status", length = 50)
    private String newStatus;

    /** Raw Paystack webhook or API response payload stored as JSONB */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static PaymentEvent of(Payment payment, PaymentEventType eventType,
                                  PaymentStatus oldStatus, PaymentStatus newStatus,
                                  JsonNode payload) {
        PaymentEvent e  = new PaymentEvent();
        e.payment    = payment;
        e.eventType     = eventType;
        e.oldStatus     = oldStatus  != null ? oldStatus.name()  : null;
        e.newStatus     = newStatus  != null ? newStatus.name()  : null;
        e.payload       = payload;
        e.createdAt     = Instant.now();
        return e;
    }

    public static PaymentEvent statusChange(
            Payment payment,
            PaymentStatus oldStatus,
            PaymentStatus newStatus,
            JsonNode payload
    ) {
        return of(payment,
                PaymentEventType.WEBHOOK_RECEIVED,
                oldStatus,
                newStatus,
                payload
        );
    }

    public static PaymentEvent initiated(Payment payment) {
        return of(payment,
                PaymentEventType.PAYMENT_INITIATED,
                null,
                payment.getStatus(),
                null
        );
    }

    public static PaymentEvent requestSent(Payment payment, JsonNode payload) {
        return of(payment,
                PaymentEventType.REQUEST_TO_PAY_SENT,
                null,
                payment.getStatus(),
                payload
        );
    }

    public static PaymentEvent failed(
            Payment payment,
            PaymentStatus oldStatus,
            JsonNode reason
    ) {
        return of(payment,
                PaymentEventType.PAYMENT_FAILED,
                oldStatus,
                PaymentStatus.FAILED,
                reason
        );
    }
}
