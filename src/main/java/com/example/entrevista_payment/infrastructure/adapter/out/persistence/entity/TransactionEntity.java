package com.example.entrevista_payment.infrastructure.adapter.out.persistence.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Container(containerName = "transactions")
public class TransactionEntity {
    @Id
    private String id;

    @PartitionKey
    private String orderId;

    private String customerId;
    private Double amount;
    private String status;
    private Instant timestamp;
    private Instant processedAt;
}