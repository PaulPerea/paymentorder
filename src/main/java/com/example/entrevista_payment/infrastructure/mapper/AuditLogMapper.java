package com.example.entrevista_payment.infrastructure.mapper;


import com.example.entrevista_payment.domain.model.AuditLog;
import com.example.entrevista_payment.infrastructure.adapter.out.persistence.entity.AuditLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogMapper {

    private final OrderMapper orderMapper;

    public AuditLogDto toDto(AuditLog auditLog) {
        AuditLogDto dto = AuditLogDto.builder()
                .auditId(auditLog.getAuditId())
                .timestamp(auditLog.getTimestamp())
                .eventType(auditLog.getEventType())
                .status(auditLog.getStatus())
                .processingTime(auditLog.getProcessingTime())
                .build();

        if (auditLog.getOrder() != null) {
            dto.setOrder(orderMapper.toDto(auditLog.getOrder()));
        }

        if (auditLog.getTransaction() != null) {
            dto.setTransaction(toTransactionDto(auditLog.getTransaction()));
        }

        return dto;
    }

    private AuditLogDto.TransactionDto toTransactionDto(
            com.example.entrevista_payment.domain.model.Transaction transaction) {
        return AuditLogDto.TransactionDto.builder()
                .id(transaction.getId().getValue())
                .orderId(transaction.getOrderId().getValue())
                .customerId(transaction.getCustomerId().getValue())
                .amount(transaction.getAmount().getAmount().doubleValue())
                .status(transaction.getStatus().getValue())
                .timestamp(transaction.getTimestamp())
                .processedAt(transaction.getProcessedAt())
                .build();
    }
}