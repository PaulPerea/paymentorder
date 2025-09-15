package com.example.entrevista_payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record Transaction(
        String id,

        @JsonProperty("order_id")
        String orderId,

        @JsonProperty("customer_id")
        String customerId,

        BigDecimal amount,

        TransactionStatus status,

        @JsonProperty("processed_at")
        Instant processedAt,

        @JsonProperty("items_count")
        int itemsCount
) {

    public static Transaction from(Order order, TransactionStatus status) {
        return Transaction.builder()
                .id(UUID.randomUUID().toString())
                .orderId(order.orderId())
                .customerId(order.customerId())
                .amount(order.totalAmount())
                .status(status)
                .processedAt(Instant.now())
                .itemsCount(order.items().size())
                .build();
    }

    public enum TransactionStatus {
        PENDING,
        PROCESSED,
        FAILED
    }
}
