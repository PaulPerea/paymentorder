package com.example.entrevista_payment.domain.model;

import com.example.entrevista_payment.domain.model.valueobjects.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Transaction {
    private final TransactionId id;
    private final OrderId orderId;
    private final CustomerId customerId;
    private final Money amount;
    private final TransactionStatus status;
    private final Instant timestamp;
    private final Instant processedAt;

    private Transaction(Builder builder) {
        this.id = builder.id;
        this.orderId = builder.orderId;
        this.customerId = builder.customerId;
        this.amount = builder.amount;
        this.status = builder.status;
        this.timestamp = builder.timestamp;
        this.processedAt = builder.processedAt;
    }

    public static Transaction createFromOrder(Order order) {
        return new Builder()
                .withId(new TransactionId(UUID.randomUUID().toString()))
                .withOrderId(order.getOrderId())
                .withCustomerId(order.getCustomerId())
                .withAmount(order.getTotalAmount())
                .withStatus(TransactionStatus.COMPLETED)
                .withTimestamp(Instant.now())
                .withProcessedAt(Instant.now())
                .build();
    }

    public TransactionId getId() { return id; }
    public OrderId getOrderId() { return orderId; }
    public CustomerId getCustomerId() { return customerId; }
    public Money getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public Instant getTimestamp() { return timestamp; }
    public Instant getProcessedAt() { return processedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Builder {
        private TransactionId id;
        private OrderId orderId;
        private CustomerId customerId;
        private Money amount;
        private TransactionStatus status;
        private Instant timestamp;
        private Instant processedAt;

        public Builder withId(TransactionId id) {
            this.id = id;
            return this;
        }

        public Builder withOrderId(OrderId orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder withCustomerId(CustomerId customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder withAmount(Money amount) {
            this.amount = amount;
            return this;
        }

        public Builder withStatus(TransactionStatus status) {
            this.status = status;
            return this;
        }

        public Builder withTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withProcessedAt(Instant processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public Transaction build() {
            Objects.requireNonNull(id, "Transaction ID es requerida");
            Objects.requireNonNull(orderId, "Order ID es requerida");
            Objects.requireNonNull(customerId, "Customer ID es requerida");
            Objects.requireNonNull(amount, "Amount es requerida");
            Objects.requireNonNull(status, "Status es requerida");
            Objects.requireNonNull(timestamp, "Timestamp es requerida");
            return new Transaction(this);
        }
    }
}