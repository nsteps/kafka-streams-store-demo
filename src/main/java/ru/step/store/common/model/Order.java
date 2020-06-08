package ru.step.store.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    private UUID id;
    private Status status;
    private Product product;
    private int quantity;
    private long price;
    private UUID customerId;

    public enum Status {CREATED, VALIDATED, FAILED, SHIPPED}

    public enum Product {SHOES, WATCH, GUITAR}
}
