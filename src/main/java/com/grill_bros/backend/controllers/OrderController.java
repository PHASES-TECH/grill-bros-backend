package com.grill_bros.backend.controllers;

import com.grill_bros.backend.common.ApiResponse;
import com.grill_bros.backend.dto.ordersdto.CreateOrderRequest;
import com.grill_bros.backend.dto.ordersdto.OrderResponse;
import com.grill_bros.backend.dto.ordersdto.UpdateOrderStatusRequest;
import com.grill_bros.backend.service.orderservice.OrderService;
import com.grill_bros.backend.service.utilsservice.ReceiptService;
import io.lettuce.core.dynamic.annotation.Param;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Public order creation and status tracking — no auth required")
public class OrderController {

    private final OrderService orderService;
    private final ReceiptService receiptService;

    @PostMapping
    @Operation(summary = "Create a new order from cart contents", description = "Submits the customer's cart at checkout. Cart is managed on the " + "frontend; this endpoint receives the final resolved list of items. " + "Returns the created order including its UUID (needed for payment) " + "and human-readable order number (needed for tracking).")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created successfully"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error — missing or invalid fields"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "One or more menu items not found"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "One or more items currently unavailable")})
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Customer details and cart items", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                {
                  "customerName": "Kwame Mensah",
                  "customerPhone": "0244123456",
                  "customerEmail": "kwame@example.com",
                  "notes": "Extra spicy please",
                  "items": [
                    { "menuItemId": "3fa85f64-5717-4562-b3fc-2c963f66afa6", "quantity": 2 },
                    { "menuItemId": "7ab12c34-1234-5678-abcd-ef1234567890", "quantity": 1 }
                  ]
                }
            """))) CreateOrderRequest req) {

        log.info("Checkout request received for phone={} items={}", req.getCustomerPhone(), req.getItems().size());

        OrderResponse order = orderService.createOrder(req);

        log.info("Order created successfully: orderNumber={} totalAmount={}", order.getOrderNumber(), order.getTotalAmount());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(order));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Track an order by order number", description = "Returns the current status and full line items for an order. " + "Order numbers follow the format QB-YYYYMMDD-XXXX. " + "Use this to build a customer-facing order tracking page.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order found"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")})
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@Parameter(description = "Human-readable order number (e.g. QB-20250601-0042)", example = "QB-20250601-0042", required = true) @PathVariable String orderNumber) {

        log.debug("Order status lookup: orderNumber={}", orderNumber);

        return ResponseEntity.ok(ApiResponse.ok(orderService.getByOrderNumber(orderNumber)));
    }

    @PatchMapping("")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @RequestParam(required = true) String orderId,
            @RequestBody UpdateOrderStatusRequest req) {

        return ResponseEntity.ok(ApiResponse.ok(orderService.updateStatus(orderId, req)));
    }

    @GetMapping("/receipt/{id}")
    public ResponseEntity<?> generateReceipt(@PathVariable("id")  String orderId) {
        receiptService.adminGenerateAndSendReceipt(orderId);
        return ResponseEntity.ok(
                Map.of("message", "Receipt generated successfully")
        );
    }
}
