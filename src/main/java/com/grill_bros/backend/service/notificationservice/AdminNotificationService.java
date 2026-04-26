package com.grill_bros.backend.service.notificationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grill_bros.backend.cache.RedisKeys;
import com.grill_bros.backend.events.OrderCreatedEvent;
import com.grill_bros.backend.events.OrderStatusChangedEvent;
import com.grill_bros.backend.events.PaymentCompletedEvent;
import com.grill_bros.backend.service.cacheservice.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final CacheService cache;
    private final ObjectMapper objectMapper;

    /** Registry of currently connected admin SSE clients */
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // ── Subscription management ───────────────────────────────────────────────

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout — client reconnects

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(()    -> emitters.remove(emitter));
        emitter.onError(e      -> emitters.remove(emitter));

        emitters.add(emitter);
        log.debug("Admin SSE client connected. Total: {}", emitters.size());

        // Deliver any missed events from Redis queue
        drainMissedEvents(emitter);

        return emitter;
    }

    // ── Event listeners ───────────────────────────────────────────────────────

    @Async
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        broadcast("NEW_ORDER", Map.of(
                "orderId",      event.getOrderId(),
                "orderNumber",  event.getOrderNumber(),
                "customerName", event.getCustomerName(),
                "totalAmount",  event.getTotalAmount()
        ));
    }

    @Async
    @EventListener
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        String type = event.getStatus().name().equals("SUCCESSFUL")
                ? "PAYMENT_RECEIVED" : "PAYMENT_FAILED";

        broadcast(type, Map.of(
                "orderId",       event.getOrderId(),
                "orderNumber",   event.getOrderNumber(),
                "status",        event.getStatus(),
                "amount",        event.getAmount(),
                "phoneNumber",   event.getPhoneNumber(),
                "failureReason", event.getFailureReason() != null ? event.getFailureReason() : ""
        ));
    }

    @Async
    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        broadcast("ORDER_STATUS_CHANGED", Map.of(
                "orderId",     event.getOrderId(),
                "orderNumber", event.getOrderNumber(),
                "oldStatus",   event.getOldStatus(),
                "newStatus",   event.getNewStatus()
        ));
    }

    // ── Internal broadcast ────────────────────────────────────────────────────

    private void broadcast(String eventType, Object payload) {
        String json = serialize(payload);

        // Persist to Redis in case no admins are connected
        cache.pushToList(RedisKeys.ADMIN_NOTIFY_QUEUE,
                eventType + "::" + json,
                RedisKeys.TTL_NOTIFY_QUEUE_SECONDS);

        // Invalidate dashboard stats cache on new order/payment
        if ("NEW_ORDER".equals(eventType) || "PAYMENT_RECEIVED".equals(eventType)) {
            cache.evict(RedisKeys.ADMIN_STATS_TODAY);
        }

        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventType)
                        .data(json));
            } catch (IOException e) {
                dead.add(emitter);
                log.debug("Removing dead SSE emitter: {}", e.getMessage());
            }
        }
        emitters.removeAll(dead);
    }

    private void drainMissedEvents(SseEmitter emitter) {
        List<String> missed = cache.popAllFromList(RedisKeys.ADMIN_NOTIFY_QUEUE);
        for (String entry : missed) {
            try {
                int sep       = entry.indexOf("::");
                String type   = entry.substring(0, sep);
                String data   = entry.substring(sep + 2);
                emitter.send(SseEmitter.event().name(type).data(data));
            } catch (IOException e) {
                log.debug("Could not deliver missed event: {}", e.getMessage());
                break;
            }
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
