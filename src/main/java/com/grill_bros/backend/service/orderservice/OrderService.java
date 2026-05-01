package com.grill_bros.backend.service.orderservice;

import com.grill_bros.backend.domain.OrderNumberGenerator;
import com.grill_bros.backend.dto.ordersdto.CreateOrderRequest;
import com.grill_bros.backend.dto.ordersdto.OrderResponse;
import com.grill_bros.backend.dto.ordersdto.UpdateOrderStatusRequest;
import com.grill_bros.backend.events.OrderCreatedEvent;
import com.grill_bros.backend.events.OrderStatusChangedEvent;
import com.grill_bros.backend.exceptions.InvalidStateTransitionException;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.*;
import com.grill_bros.backend.records.OrderStatus;
import com.grill_bros.backend.repository.*;
import com.grill_bros.backend.service.smsservice.SmsProviderService;
import com.grill_bros.backend.service.userservice.AuthContextService;
import com.grill_bros.backend.specifications.OrderSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final ModifierRepository modifierRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest req) {
        Order order = buildOrder(req, null);
        addCustomer(req);
        Order saved = orderRepository.save(order);
        log.info("Order created: {} for customer: {}", saved.getOrderNumber(), saved.getCustomerPhone());
        eventPublisher.publishEvent(new OrderCreatedEvent(this, saved));
        return OrderResponse.from(saved);
    }

    public void addCustomer(CreateOrderRequest req) {

        customerRepository.findByPhoneNumber(req.getCustomerPhone())
                .orElseGet(() -> {
                    Customer customer = new Customer();
                    customer.setFullName(req.getCustomerName());
                    customer.setPhoneNumber(req.getCustomerPhone());
                    customer.setEmail(req.getCustomerEmail());

                    return customerRepository.save(customer);
                });
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

    @Transactional
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

            OrderItem orderItem = OrderItem.from(menuItem, lineReq.getQuantity());

            List<OrderItemModifier> modifiers = buildModifiers(
                    lineReq.getModifierIds(),
                    menuItem,
                    orderItem
            );

            orderItem.setModifiers(modifiers);
            order.addItem(orderItem);
        }

        String message = String.format(
                "Order confirmed! Your order ID is %s. Track your order status anytime.",
                orderNumber
        );

        smsProviderService.sendSms(List.of(req.getCustomerPhone()), message);

        order.calculateTotals();
        return order;
    }

    @Transactional
    private List<OrderItemModifier> buildModifiers(
            List<UUID> modifierIds,
            MenuItem menuItem,
            OrderItem orderItem
    ) {

        if (modifierIds == null || modifierIds.isEmpty()) return List.of();

        List<Modifier> modifiers = modifierRepository.findAllById(modifierIds);

        Set<UUID> validGroupIds = menuItem.getModifierGroups()
                .stream()
                .map(ModifierGroup::getId)
                .collect(Collectors.toSet());

        for (Modifier m : modifiers) {
            if (!validGroupIds.contains(m.getGroup().getId())) {
                throw new IllegalArgumentException("Invalid modifier for this item");
            }
        }

        return modifiers.stream()
                .map(m -> {
                    OrderItemModifier oim = new OrderItemModifier();
                    oim.setOrderItem(orderItem);
                    oim.setModifier(m);
                    oim.setPrice(m.getPrice());
                    return oim;
                })
                .collect(Collectors.toList());
    }
}
