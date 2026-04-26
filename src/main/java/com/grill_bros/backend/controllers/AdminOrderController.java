package com.grill_bros.backend.controllers;

import com.grill_bros.backend.common.ApiResponse;
import com.grill_bros.backend.common.PagedResponse;
import com.grill_bros.backend.dto.ordersdto.CreateOrderRequest;
import com.grill_bros.backend.dto.ordersdto.OrderResponse;
import com.grill_bros.backend.dto.ordersdto.UpdateOrderStatusRequest;
import com.grill_bros.backend.model.UserPrincipal;
import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.records.OrderStatus;
import com.grill_bros.backend.service.notificationservice.AdminNotificationService;
import com.grill_bros.backend.service.orderservice.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin — Orders", description = "Admin order management")
@PreAuthorize("hasAnyRole('REGULAR_ADMIN', 'SUPER_ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;
    private final AdminNotificationService notificationService;

    @GetMapping
    @Operation(summary = "List orders with optional filters (paginated, newest first)")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> listOrders(
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) OrderStatus status,

            @Parameter(description = "Filter by customer phone (partial match)")
            @RequestParam(required = false) String phone,

            @Parameter(description = "Filter by customer name (partial match)")
            @RequestParam(required = false) String customerName,

            @Parameter(description = "From date (ISO date)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "To date (ISO date)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, 100);
        Instant fromInstant = from != null
                ? from.atStartOfDay(ZoneOffset.UTC).toInstant()
                : null;

        Instant toInstant = to != null
                ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                : null;

        var pageable = PageRequest.of(page, size, Sort.by("id").descending());

        return ResponseEntity.ok(ApiResponse.ok(
                PagedResponse.of(orderService.adminListOrders(
                        status, phone, customerName, fromInstant, toInstant, pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full order detail with line items and payment info")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.adminGetOrder(id)));
    }

    // ── Status update ─────────────────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status (CONFIRMED → PREPARING → READY → COMPLETED)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrderStatusRequest req) {

        return ResponseEntity.ok(ApiResponse.ok(orderService.updateStatus(id, req)));
    }

    // ── Admin place-on-behalf ─────────────────────────────────────────────────

    @PostMapping("/place-for-customer")
    @Operation(summary = "Admin places order on behalf of a customer — triggers MoMo prompt to their phone")
    public ResponseEntity<ApiResponse<OrderResponse>> placeForCustomer(
            @Valid @RequestBody CreateOrderRequest req,
            Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Users user = userPrincipal.getUser();

        OrderResponse order = orderService.adminPlaceForCustomer(req, user.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(order));
    }

    // ── SSE live stream ───────────────────────────────────────────────────────

    @GetMapping(value = "/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time order and payment notifications (SSE)")
    public SseEmitter liveStream() {
        return notificationService.subscribe();
    }
}
