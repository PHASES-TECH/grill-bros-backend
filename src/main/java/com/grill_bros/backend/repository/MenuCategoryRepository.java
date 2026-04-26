package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

    List<MenuCategory> findAllByActiveTrueOrderByDisplayOrderAsc();

    Optional<MenuCategory> findBySlugAndActiveTrue(String slug);

    Optional<MenuCategory> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    /** Fetch categories with their active items eagerly — used for full menu endpoint */
    @Query("""
        SELECT DISTINCT c FROM MenuCategory c
        LEFT JOIN FETCH c.items i
        WHERE c.active = true
          AND (i IS NULL OR (i.active = true AND i.available = true))
        ORDER BY c.displayOrder ASC
        """)
    List<MenuCategory> findAllActiveWithItems();
}
