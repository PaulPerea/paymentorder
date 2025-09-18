package com.example.entrevista_payment.domain.model;

import com.example.entrevista_payment.domain.model.valueobjects.ProductId;

import java.util.Objects;

public class OrderItem {
    private final ProductId productId;
    private final Integer quantity;

    public OrderItem(ProductId productId, Integer quantity) {
        validateItem(productId, quantity);
        this.productId = productId;
        this.quantity = quantity;
    }

    private void validateItem(ProductId productId, Integer quantity) {
        Objects.requireNonNull(productId, "Product ID no puede ser null");
        Objects.requireNonNull(quantity, "Quantity cno puede ser null");

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity mayor a 0");
        }
    }

    public ProductId getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(productId, orderItem.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }
}