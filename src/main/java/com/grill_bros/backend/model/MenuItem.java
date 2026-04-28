package com.grill_bros.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "menu_items",
        indexes = {
                @Index(name = "idx_item_category",    columnList = "category_id"),
                @Index(name = "idx_item_available",   columnList = "is_available"),
                @Index(name = "idx_item_active",      columnList = "is_active"),
                @Index(name = "idx_item_sort",        columnList = "sort_order"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class MenuItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @EqualsAndHashCode.Include
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL)
    private List<ModifierGroup> modifierGroups = new ArrayList<>();


    public static MenuItem create(String name, BigDecimal price, MenuCategory category) {
        MenuItem item     = new MenuItem();
        item.name         = name;
        item.price        = price;
        item.category     = category;
        return item;
    }

    public void markUnavailable() { this.available = false; }
    public void markAvailable()   { this.available = true;  }
    public void softDelete()      { this.active    = false; }
}
