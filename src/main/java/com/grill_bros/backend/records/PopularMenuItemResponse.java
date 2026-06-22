package com.grill_bros.backend.records;

import java.util.UUID;

public record PopularMenuItemResponse(
        UUID menuItemId,
        String name,
        String imageUrl,
        Long totalOrdered
) {
}
