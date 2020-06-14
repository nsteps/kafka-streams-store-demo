package ru.step.store.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderValidation {
    private UUID orderId;
    private OrderValidationType type;
    private OrderValidationResult result;

    public enum OrderValidationType {FRAUD_CHECK, ORDER_DETAILS_CHECK, INVENTORY_CHECK}

    public enum OrderValidationResult {PASS, FAIL}
}
