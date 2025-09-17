package com.example.entrevista_payment;

import com.azure.spring.data.cosmos.repository.ReactiveCosmosRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends ReactiveCosmosRepository<Transaction, String> {
    // ReactiveCosmosRepository ya proporciona save(), findById(), etc. de forma reactiva
}