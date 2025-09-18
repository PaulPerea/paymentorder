package com.example.entrevista_payment.infrastructure.adapter.out.persistence.cosmos;

import com.azure.spring.data.cosmos.repository.ReactiveCosmosRepository;
import com.example.entrevista_payment.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface CosmosTransactionRepository
        extends ReactiveCosmosRepository<TransactionEntity, String> {
}
