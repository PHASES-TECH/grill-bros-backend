package com.grill_bros.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "modifier_groups")
@Getter
@Setter
@NoArgsConstructor
public class ModifierGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    // e.g. "Add-ons", "Size"
    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false)
    private int minSelections = 0;

    @Column(nullable = false)
    private int maxSelections = 1;

    @ManyToMany(mappedBy = "modifierGroups")
    private List<MenuItem> menuItems = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<Modifier> modifiers = new ArrayList<>();
}
