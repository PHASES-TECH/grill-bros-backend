package com.grill_bros.backend.model;

import com.github.f4b6a3.ulid.UlidCreator;
import com.grill_bros.backend.records.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Set when an admin places an order on behalf of a customer */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placed_by_admin_id")
    private Users placedByAdmin;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    public static Order create(String orderNumber,
                               String customerName,
                               String customerPhone,
                               String customerEmail,
                               String notes) {
        Order o         = new Order();
        o.orderNumber   = orderNumber;
        o.customerName  = customerName;
        o.customerPhone = customerPhone;
        o.customerEmail = customerEmail;
        o.notes         = notes;
        o.status        = OrderStatus.PENDING;
        return o;
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }

    public void calculateTotals() {
        this.subtotal    = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = this.subtotal;
    }

    /**
     * Guards against invalid state transitions at the domain level.
     * Throws {@link IllegalStateException} rather than a checked exception
     * so callers at the service layer can wrap with the appropriate HTTP status.
     */
    public void transitionTo(OrderStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Order " + orderNumber + " cannot transition from " + status + " to " + next);
        }
        this.status = next;
    }
}
