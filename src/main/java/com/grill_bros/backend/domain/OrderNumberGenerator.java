package com.grill_bros.backend.domain;

import com.grill_bros.backend.repository.OrderRepository;
import com.grill_bros.backend.service.cacheservice.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private final CacheService cache;
    private final OrderRepository orderRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate() {
        String todayOrder = LocalDate.now(ZoneOffset.UTC).format(DATE_FMT);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = today.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

        long todayCount = orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);

        long seq       = todayCount + 1;
        String candidate;
        int    attempts = 0;

        do {
            candidate = buildOrderNumber(todayOrder, seq);
            seq++;
            attempts++;

            // Safety valve — should never be reached in normal operation
            if (attempts > 100) {
                log.error("OrderNumberGenerator exceeded 100 attempts for date={}", today);
                throw new IllegalStateException(
                        "Unable to generate a unique order number for date: " + today);
            }
        } while (orderRepository.existsByOrderNumber(candidate));

        if (attempts > 1) {
            log.warn("Order number collision resolved after {} attempts: {}",
                    attempts, candidate);
        }

        log.debug("Generated order number: {} (seq={} todayCount={})",
                candidate, seq - 1, todayCount);
        return candidate;
    }

    private String buildOrderNumber(String date, long seq) {
        return "QB-" + date + "-" + String.format("%04d", seq);
    }
}
