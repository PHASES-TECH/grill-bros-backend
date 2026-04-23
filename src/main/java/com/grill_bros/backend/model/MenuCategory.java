package com.grill_bros.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "menu_categories",
        indexes = {
                @Index(name = "idx_category_slug",   columnList = "slug",         unique = true),
                @Index(name = "idx_category_active", columnList = "is_active"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "items")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class MenuCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @EqualsAndHashCode.Include
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MenuItem> items = new ArrayList<>();


    public static MenuCategory create(String name, String slug, int displayOrder) {
        MenuCategory c = new MenuCategory();
        c.name         = name;
        c.slug         = slug;
        c.displayOrder = displayOrder;
        return c;
    }

    public void deactivate() { this.active = false; }
    public void activate()   { this.active = true;  }
}
