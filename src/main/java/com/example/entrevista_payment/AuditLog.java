package com.example.entrevista_payment;

import lombok.Data;
import lombok.Builder;
import java.time.Instant;
import java.time.Duration;

@Data
@Builder
public class AuditLog {
    private String auditId;
    private Instant timestamp;
    private String eventType;
    private String status;
    private Order order;
    private Transaction transaction;
    private Duration processingTime;
    private String error;
}