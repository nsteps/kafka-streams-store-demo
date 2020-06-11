package ru.step.store.order.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.step.store.common.model.Order;
import ru.step.store.order.model.ErrorResponse;
import ru.step.store.order.model.OrderCreateRequest;
import ru.step.store.order.service.OrderService;

import java.util.UUID;

import static ru.step.store.common.model.Order.Status.VALIDATED;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    final OrderService orderService;

    /**
     * Get order current state.
     */
    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable UUID orderId) {
        return orderService.getOrderOrNull(orderId, it -> true);
    }

    /**
     * Long-poling get. Blocks until order as been validated with some timeout.
     */
    @GetMapping("/{orderId}/validated")
    public ResponseEntity<?> getOrderValidated(@PathVariable UUID orderId) {
        final Order order = orderService.getOrderOrNull(orderId, it -> it.getStatus().equals(VALIDATED));
        return order != null
                ? ResponseEntity.ok(order)
                : ResponseEntity.status(HttpStatus.LOCKED).body(
                        new ErrorResponse("Order " + orderId + " is not validated yet"));
    }

    /**
     * Create order.
     */
    @PostMapping
    public ResponseEntity<?> submit(@RequestBody OrderCreateRequest request) {
        orderService.submitOrder(request);
        return ResponseEntity.accepted().build();
    }
}
