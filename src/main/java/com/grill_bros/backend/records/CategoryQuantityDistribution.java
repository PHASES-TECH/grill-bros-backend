package com.grill_bros.backend.records;

public record CategoryQuantityDistribution(
        String category,
        Integer totalQuantity,
        double percentage
) {}
