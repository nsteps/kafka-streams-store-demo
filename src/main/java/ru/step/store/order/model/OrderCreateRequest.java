package ru.step.store.order.model;

import lombok.Data;
import ru.step.store.common.model.Order;

import java.util.UUID;

@Data
public class OrderCreateRequest {
    private UUID id;
    private Order.Product product;
    private int quantity;
    private long price;
}
