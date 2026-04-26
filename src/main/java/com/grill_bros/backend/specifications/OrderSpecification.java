package com.grill_bros.backend.specifications;

import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.records.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    public static Specification<Order> filter(
            OrderStatus status,
            String phone,
            String customerName,
            Instant from,
            Instant to
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (phone != null && !phone.isBlank()) {
                predicates.add(
                        cb.like(root.get("customerPhone"), "%" + phone + "%")
                );
            }

            if (customerName != null && !customerName.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("customerName")),
                                "%" + customerName.toLowerCase() + "%"
                        )
                );
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
