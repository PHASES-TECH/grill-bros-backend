package com.grill_bros.backend.domain;

import com.grill_bros.backend.repository.OrderRepository;
import com.grill_bros.backend.service.cacheservice.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private final CacheService cache;
    private final OrderRepository orderRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate() {
        String today   = LocalDate.now().format(DATE_FMT);
        String redisKey = "quickbite:ordernumber:seq:" + today;

        long   seq      = cache.increment(redisKey, 86_400); // TTL = 1 day
        String candidate = "QB-" + today + "-" + String.format("%04d", seq);

        // Extremely unlikely collision guard (only matters after Redis flush)
        if (orderRepository.existsByOrderNumber(candidate)) {
            candidate = candidate + "-X";
        }
        return candidate;
    }
}
