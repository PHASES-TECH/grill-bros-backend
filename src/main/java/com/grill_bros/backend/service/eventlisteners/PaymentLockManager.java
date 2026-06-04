package com.grill_bros.backend.service.eventlisteners;

import com.grill_bros.backend.cache.RedisKeys;
import com.grill_bros.backend.service.cacheservice.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentLockManager {

    private final CacheService cache;

    public boolean lockOrder(String orderId) {
        return cache.acquireLock(
                RedisKeys.orderLock(orderId),
                RedisKeys.TTL_ORDER_LOCK_SECONDS
        );
    }

    public void unlockOrder(String orderId) {
        cache.releaseLock(
                RedisKeys.orderLock(orderId)
        );
    }
}
