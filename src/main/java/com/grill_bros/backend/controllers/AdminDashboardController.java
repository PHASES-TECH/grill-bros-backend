package com.grill_bros.backend.controllers;

import com.grill_bros.backend.common.ApiResponse;
import com.grill_bros.backend.dto.dashboarddtos.DashboardStatsResponse;
import com.grill_bros.backend.dto.dashboarddtos.RevenueResponse;
import com.grill_bros.backend.dto.dashboarddtos.TopItemResponse;
import com.grill_bros.backend.service.dashboardservice.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Admin analytics and stats endpoints")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/today")
    @Operation(summary = "Get today's dashboard stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getTodayStats() {

        return ResponseEntity.ok(
                ApiResponse.ok(dashboardService.getTodayStats())
        );
    }

    // ── Revenue by Date Range ───────────────────────────────────
    @GetMapping("/revenue")
    @Operation(summary = "Get revenue within a date range")
    public ResponseEntity<ApiResponse<RevenueResponse>> getRevenue(
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after To date");
        }

        return ResponseEntity.ok(
                ApiResponse.ok(dashboardService.getRevenue(from, to))
        );
    }

    // ── Top Selling Items ───────────────────────────────────────
    @GetMapping("/top-items")
    @Operation(summary = "Get top selling menu items (last 30 days)")
    public ResponseEntity<ApiResponse<List<TopItemResponse>>> getTopItems(
            @Parameter(description = "Number of items to return (max 20)")
            @RequestParam(defaultValue = "5") int limit
    ) {

        limit = Math.min(limit, 20);

        return ResponseEntity.ok(
                ApiResponse.ok(dashboardService.getTopItems(limit))
        );
    }

    @GetMapping("/category/quantity-dist")
    public ResponseEntity<ApiResponse<?>> getCategoryQuantityDistribution() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCategoryQuantityDistribution()));
    }

    @GetMapping("/category/revenue-dist")
    public ResponseEntity<ApiResponse<?>> getCategoryRevenueDistribution() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCategoryRevenueDistribution()));
    }
}
