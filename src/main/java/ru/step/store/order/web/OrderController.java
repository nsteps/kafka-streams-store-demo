package ru.step.store.order.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.step.store.common.model.Order;
import ru.step.store.order.model.OrderCreateRequest;
import ru.step.store.order.service.OrderService;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    final OrderService orderService;

    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable UUID orderId) {
        return orderService.getOrder(orderId);
    }

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody OrderCreateRequest request) {
        orderService.submitOrder(request);
        return ResponseEntity.accepted().build();
    }
}
