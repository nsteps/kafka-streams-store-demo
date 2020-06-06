package ru.step.store.common.model;

import lombok.Data;

import java.util.UUID;

@Data
public class Order {
    final private UUID id;
    final private Status status;
    final private Product product;
    final int quantity;
    final long price;
    final UUID customerId;


    public enum Status {CREATED, VALIDATED, FAILED, SHIPPED}

    public enum Product {SHOES, WATCH, GUITAR}
}
