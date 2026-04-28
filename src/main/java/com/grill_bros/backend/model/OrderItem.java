package com.grill_bros.backend.model;

import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_item_order",    columnList = "order_id"),
                @Index(name = "idx_order_item_menuitem", columnList = "menu_item_id"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class OrderItem extends BaseEntity {

    @Id
    @Column(length = 26, nullable = false, updatable = false)
    private String id;

    @PrePersist
    public void generateId() {
        if (id == null) {
            id = UlidCreator.getUlid().toString();
        }
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Nullable to allow MenuItem to be soft-deleted without breaking order history */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    /** Snapshot — never read from menuItem.name after order creation */
    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    /** Snapshot of price at order time */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL)
    private List<OrderItemModifier> modifiers = new ArrayList<>();

    public static OrderItem from(MenuItem menuItem, int quantity) {
        OrderItem oi  = new OrderItem();
        oi.menuItem   = menuItem;
        oi.itemName   = menuItem.getName();
        oi.unitPrice  = menuItem.getPrice();
        oi.quantity   = quantity;
        return oi;
    }

    public BigDecimal calculateLineTotal() {

        BigDecimal base = unitPrice.multiply(BigDecimal.valueOf(quantity));

        BigDecimal modifiersTotal = modifiers.stream()
                .map(OrderItemModifier::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(quantity));

        this.lineTotal = base.add(modifiersTotal);

        return this.lineTotal;
    }
}