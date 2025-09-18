package com.example.entrevista_payment.infrastructure.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private String auditId;
    private Instant timestamp;
    private String eventType;
    private String status;
    private OrderDto order;
    private TransactionDto transaction;
    private Duration processingTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionDto {
        private String id;
        private String orderId;
        private String customerId;
        private Double amount;
        private String status;
        private Instant timestamp;
        private Instant processedAt;
    }
}