package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.OrderItem;
import com.grill_bros.backend.records.PopularMenuItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

    @Query("""
    SELECT new com.grill_bros.backend.records.PopularMenuItemResponse(
        m.id,
        m.name,
        m.imageUrl,
        SUM(oi.quantity)
    )
    FROM OrderItem oi
    JOIN oi.menuItem m
    JOIN oi.order o
    WHERE o.status = 'COMPLETED'
    GROUP BY m.id, m.name, m.imageUrl
    ORDER BY SUM(oi.quantity) DESC
""")
    Page<PopularMenuItemResponse> findMostPopularItems(Pageable pageable);
}
