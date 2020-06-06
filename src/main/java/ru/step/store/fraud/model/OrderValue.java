package ru.step.store.fraud.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.step.store.common.model.Order;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderValue {
    private Order order;
    private long totalValue;
}
