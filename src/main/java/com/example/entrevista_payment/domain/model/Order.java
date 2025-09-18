package com.example.entrevista_payment.domain.model;

import com.example.entrevista_payment.domain.model.valueobjects.CustomerId;
import com.example.entrevista_payment.domain.model.valueobjects.Money;
import com.example.entrevista_payment.domain.model.valueobjects.OrderId;

import java.util.List;
import java.util.Objects;

public class Order {
    private final OrderId orderId;
    private final CustomerId customerId;
    private final List<OrderItem> items;
    private final Money totalAmount;

    public Order(OrderId orderId, CustomerId customerId, List<OrderItem> items,
                 Money totalAmount) {
        validateOrder(orderId, customerId, items, totalAmount);
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = List.copyOf(items);
        this.totalAmount = totalAmount;
    }

    private void validateOrder(OrderId orderId, CustomerId customerId,
                               List<OrderItem> items, Money totalAmount) {
        Objects.requireNonNull(orderId, "Order ID no puede ser null");
        Objects.requireNonNull(customerId, "Customer ID no puede ser null");
        Objects.requireNonNull(totalAmount, "Total amount no puede ser null");

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Orden mayor a 1 item");
        }

        if (totalAmount.isNegativeOrZero()) {
            throw new IllegalArgumentException("Total amount numero mayor a 0");
        }
    }

    public OrderId getOrderId() { return orderId; }
    public CustomerId getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
    public Money getTotalAmount() { return totalAmount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
}