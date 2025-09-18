package com.example.entrevista_payment.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class AuditLog {
    private final String auditId;
    private final Instant timestamp;
    private final String eventType;
    private final String status;
    private final Order order;
    private final Transaction transaction;
    private final Duration processingTime;

    private AuditLog(Builder builder) {
        this.auditId = builder.auditId;
        this.timestamp = builder.timestamp;
        this.eventType = builder.eventType;
        this.status = builder.status;
        this.order = builder.order;
        this.transaction = builder.transaction;
        this.processingTime = builder.processingTime;
    }

    public static AuditLog createSuccessAudit(Order order, Transaction transaction,
                                              Instant startTime) {
        return new Builder()
                .withAuditId(UUID.randomUUID().toString())
                .withTimestamp(Instant.now())
                .withEventType("PAYMENT_PROCESSED")
                .withStatus("SUCCESS")
                .withOrder(order)
                .withTransaction(transaction)
                .withProcessingTime(Duration.between(startTime, Instant.now()))
                .build();
    }

    public String getAuditId() { return auditId; }
    public Instant getTimestamp() { return timestamp; }
    public String getEventType() { return eventType; }
    public String getStatus() { return status; }
    public Order getOrder() { return order; }
    public Transaction getTransaction() { return transaction; }
    public Duration getProcessingTime() { return processingTime; }

    public static class Builder {
        private String auditId;
        private Instant timestamp;
        private String eventType;
        private String status;
        private Order order;
        private Transaction transaction;
        private Duration processingTime;

        public Builder withAuditId(String auditId) {
            this.auditId = auditId;
            return this;
        }

        public Builder withTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withEventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder withStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder withOrder(Order order) {
            this.order = order;
            return this;
        }

        public Builder withTransaction(Transaction transaction) {
            this.transaction = transaction;
            return this;
        }

        public Builder withProcessingTime(Duration processingTime) {
            this.processingTime = processingTime;
            return this;
        }

        public AuditLog build() {
            return new AuditLog(this);
        }
    }
}