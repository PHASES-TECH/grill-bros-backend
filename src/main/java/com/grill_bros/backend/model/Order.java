package com.grill_bros.backend.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.grill_bros.backend.records.OrderStatus;
import com.grill_bros.backend.records.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_number",   columnList = "order_number"),
                @Index(name = "idx_order_status",   columnList = "status"),
                @Index(name = "idx_order_phone",    columnList = "customer_phone"),
                @Index(name = "idx_order_created",  columnList = "created_at"),
                @Index(name = "idx_order_user",     columnList = "user_id"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "items")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Order extends BaseEntity {

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
    @Column(name = "order_number", nullable = false, unique = true, length = 20)
    private String orderNumber;

    /** Nullable FK — Phase 2 will populate this for logged-in customers */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CONFIRMED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20, nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.MOBILE_MONEY;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "tracking_token", unique = true, length = 12)
    private String trackingToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placed_by_admin_id")
    private Users placedByAdmin;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<OrderItem> items = new HashSet<>();

    public static Order create(String orderNumber,
                               String customerName,
                               String customerPhone,
                               String customerEmail,
                               String notes,
                               String trackingToken,
                               PaymentMethod paymentMethod) {
        Order o         = new Order();
        o.orderNumber   = orderNumber;
        o.customerName  = customerName;
        o.customerPhone = customerPhone;
        o.customerEmail = customerEmail;
        o.notes         = notes;
        o.status        = OrderStatus.CONFIRMED;
        o.trackingToken = trackingToken;
        o.paymentMethod = paymentMethod;
        return o;
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }

    public void calculateTotals() {
        this.subtotal = items.stream()
                .map(OrderItem::calculateLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalAmount = this.subtotal;
    }

    public void transitionTo(OrderStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Order " + orderNumber + " cannot transition from " + status + " to " + next);
        }
        this.status = next;
    }
}
