package com.example.entrevista_payment;

import lombok.Data;
import lombok.Builder;
import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import org.springframework.data.annotation.Id;
import java.time.Instant;

// Modelo para la Transacción que se guarda en Cosmos DB
@Data
@Builder
@Container(containerName = "transactions")
public class Transaction {
    @Id
    private String id;

    @PartitionKey
    private String orderId;

    private String customerId;
    private Double amount;
    private String status; // COMPLETED, FAILED
    private Instant timestamp;
    private Instant processedAt;
}