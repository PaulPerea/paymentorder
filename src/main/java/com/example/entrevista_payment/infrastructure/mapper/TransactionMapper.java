package com.example.entrevista_payment.infrastructure.mapper;

import com.example.entrevista_payment.domain.model.*;
import com.example.entrevista_payment.domain.model.valueobjects.*;
import com.example.entrevista_payment.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionEntity toEntity(Transaction transaction) {
        return TransactionEntity.builder()
                .id(transaction.getId().getValue())
                .orderId(transaction.getOrderId().getValue())
                .customerId(transaction.getCustomerId().getValue())
                .amount(transaction.getAmount().getAmount().doubleValue())
                .status(transaction.getStatus().getValue())
                .timestamp(transaction.getTimestamp())
                .processedAt(transaction.getProcessedAt())
                .build();
    }

    public Transaction toDomain(TransactionEntity entity) {
        return new Transaction.Builder()
                .withId(new TransactionId(entity.getId()))
                .withOrderId(new OrderId(entity.getOrderId()))
                .withCustomerId(new CustomerId(entity.getCustomerId()))
                .withAmount(new Money(entity.getAmount()))
                .withStatus(TransactionStatus.fromValue(entity.getStatus()))
                .withTimestamp(entity.getTimestamp())
                .withProcessedAt(entity.getProcessedAt())
                .build();
    }
}