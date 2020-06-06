package ru.step.store.order.mapper;

import ru.step.store.common.model.Order;
import ru.step.store.order.model.OrderCreateRequest;

import java.util.UUID;

public class OrderMapper {
    public static Order toOrder(OrderCreateRequest request) {
        return new Order(request.getId(), Order.Status.CREATED, request.getProduct(), request.getQuantity(),
                request.getPrice(), UUID.randomUUID());
    }
}
