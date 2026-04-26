package com.grill_bros.backend.service.dashboardservice;

import com.grill_bros.backend.cache.RedisKeys;
import com.grill_bros.backend.dto.dashboarddtos.DashboardStatsResponse;
import com.grill_bros.backend.dto.dashboarddtos.RevenueResponse;
import com.grill_bros.backend.dto.dashboarddtos.TopItemResponse;
import com.grill_bros.backend.records.OrderStatus;
import com.grill_bros.backend.repository.MenuItemRepository;
import com.grill_bros.backend.repository.OrderRepository;
import com.grill_bros.backend.service.cacheservice.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final CacheService cache;

    public DashboardStatsResponse getTodayStats() {
        return cache.get(RedisKeys.ADMIN_STATS_TODAY, DashboardStatsResponse.class)
                .orElseGet(() -> {
                    DashboardStatsResponse stats = computeTodayStats();
                    cache.set(RedisKeys.ADMIN_STATS_TODAY, stats, RedisKeys.TTL_STATS_SECONDS);
                    return stats;
                });
    }

    private DashboardStatsResponse computeTodayStats() {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant now        = Instant.now();

        long       completedCount = orderRepository.countCompletedBetween(startOfDay, now);
        BigDecimal revenue        = orderRepository.sumRevenueBetween(startOfDay, now);
        BigDecimal avgValue       = orderRepository.avgOrderValueBetween(startOfDay, now);

        // Order distribution by status since start of day
        List<Object[]> rawCounts  = orderRepository.countByStatusSince(startOfDay);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) byStatus.put(s.name(), 0L);
        for (Object[] row : rawCounts) {
            byStatus.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        long pendingCount  = byStatus.getOrDefault(OrderStatus.PENDING.name(),   0L);
        long completedTotal = byStatus.getOrDefault(OrderStatus.COMPLETED.name(), 0L);

        return DashboardStatsResponse.builder()
                .todayOrderCount(completedCount)
                .todayRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                .avgOrderValue(avgValue != null
                        ? avgValue.setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .pendingOrders(pendingCount)
                .completedOrders(completedTotal)
                .ordersByStatus(byStatus)
                .build();
    }

    public RevenueResponse getRevenue(LocalDate from, LocalDate to) {
        String cacheKey = RedisKeys.adminRevenueRange(from.toString(), to.toString());

        return cache.get(cacheKey, RevenueResponse.class)
                .orElseGet(() -> {
                    RevenueResponse result = computeRevenue(from, to);
                    cache.set(cacheKey, result, RedisKeys.TTL_STATS_SECONDS);
                    return result;
                });
    }

    private RevenueResponse computeRevenue(LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal totalRevenue = orderRepository.sumRevenueBetween(fromInstant, toInstant);
        long       totalOrders  = orderRepository.countCompletedBetween(fromInstant, toInstant);
        BigDecimal avgValue     = orderRepository.avgOrderValueBetween(fromInstant, toInstant);

        List<Object[]> rawDaily = orderRepository.dailyRevenueBetween(fromInstant, toInstant);
        List<RevenueResponse.DailyRevenue> daily = rawDaily.stream()
                .map(row -> RevenueResponse.DailyRevenue.builder()
                        .day((LocalDate) row[0])
                        .orderCount(((Number) row[1]).longValue())
                        .revenue(new BigDecimal(row[2].toString()))
                        .build())
                .collect(Collectors.toList());

        return RevenueResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalOrders(totalOrders)
                .avgOrderValue(avgValue != null
                        ? avgValue.setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .dailyBreakdown(daily)
                .build();
    }

    public List<TopItemResponse> getTopItems(int limit) {
        Instant since = Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        return menuItemRepository
                .findTopItemsRaw(since, PageRequest.of(0, Math.min(limit, 20)))
                .stream()
                .map(row -> TopItemResponse.builder()
                        .menuItemId(row[0] != null ? (UUID) row[0] : null)
                        .itemName(row[1].toString())
                        .totalQuantitySold(((Number) row[2]).longValue())
                        .totalRevenue(new BigDecimal(row[3].toString()))
                        .build())
                .collect(Collectors.toList());
    }
}