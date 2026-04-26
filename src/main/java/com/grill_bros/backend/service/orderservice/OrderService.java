package com.grill_bros.backend.service.orderservice;

import com.grill_bros.backend.domain.OrderNumberGenerator;
import com.grill_bros.backend.dto.ordersdto.CreateOrderRequest;
import com.grill_bros.backend.dto.ordersdto.OrderResponse;
import com.grill_bros.backend.dto.ordersdto.UpdateOrderStatusRequest;
import com.grill_bros.backend.events.OrderCreatedEvent;
import com.grill_bros.backend.events.OrderStatusChangedEvent;
import com.grill_bros.backend.exceptions.InvalidStateTransitionException;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.MenuItem;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.OrderItem;
import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.records.OrderStatus;
import com.grill_bros.backend.repository.MenuItemRepository;
import com.grill_bros.backend.repository.OrderRepository;
import com.grill_bros.backend.repository.UserRepository;
import com.grill_bros.backend.service.notificationservice.AdminNotificationService;
import com.grill_bros.backend.service.smsservice.SmsProviderService;
import com.grill_bros.backend.specifications.OrderSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository adminUserRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final SmsProviderService smsProviderService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest req) {
        Order order = buildOrder(req, null);
        Order saved = orderRepository.save(order);
        log.info("Order created: {} for customer: {}", saved.getOrderNumber(), saved.getCustomerPhone());
        eventPublisher.publishEvent(new OrderCreatedEvent(this, saved));
        return OrderResponse.from(saved);
    }

    public OrderResponse getByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumberWithItems(orderNumber)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order"));
    }

    public Page<OrderResponse> adminListOrders(OrderStatus status,
                                               String phone,
                                               String customerName,
                                               Instant from,
                                               Instant to,
                                               Pageable pageable) {
        return orderRepository
                .findAll(OrderSpecification.filter(status, phone, customerName, from, to), pageable)
                .map(OrderResponse::summary);
    }

    public OrderResponse adminGetOrder(String id) {
        return orderRepository.findByIdWithItems(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order"));
    }

    @Transactional
    public OrderResponse updateStatus(String id, UpdateOrderStatusRequest req) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order"));

        OrderStatus previous = order.getStatus();
        try {
            order.transitionTo(req.getStatus());
        } catch (IllegalStateException e) {
            throw new InvalidStateTransitionException(e.getMessage());
        }

        Order saved = orderRepository.save(order);
        eventPublisher.publishEvent(
                new OrderStatusChangedEvent(this, saved.getId(), saved.getOrderNumber(),
                        previous, saved.getStatus()));
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse adminPlaceForCustomer(CreateOrderRequest req, UUID adminId) {
        Users admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser"));

        Order order = buildOrder(req, admin);
        Order saved = orderRepository.save(order);
        log.info("Admin {} placed order {} on behalf of {}",
                admin.getEmail(), saved.getOrderNumber(), saved.getCustomerPhone());
        eventPublisher.publishEvent(new OrderCreatedEvent(this, saved));
        return OrderResponse.from(saved);
    }

    private Order buildOrder(CreateOrderRequest req, Users placedBy) {
        String orderNumber = orderNumberGenerator.generate();
        Order order = Order.create(
                orderNumber,
                req.getCustomerName(),
                req.getCustomerPhone(),
                req.getCustomerEmail(),
                req.getNotes());

        order.setPlacedByAdmin(placedBy);

        for (CreateOrderRequest.OrderItemRequest lineReq : req.getItems()) {
            MenuItem menuItem = menuItemRepository
                    .findByIdAndActiveTrue(lineReq.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "MenuItem"));

            if (!menuItem.isAvailable()) {
                throw new InvalidStateTransitionException(
                        "Item '" + menuItem.getName() + "' is currently unavailable");
            }
            order.addItem(OrderItem.from(menuItem, lineReq.getQuantity()));
        }

        String message = String.format(
                "Order confirmed! Your order ID is %s. Track your order status anytime.",
                orderNumber
        );

        smsProviderService.sendSms(List.of(req.getCustomerPhone()), message);

        order.calculateTotals();
        return order;
    }
}
