package com.example.entrevista_payment.domain.port.out;

import com.example.entrevista_payment.domain.model.Transaction;
import reactor.core.publisher.Mono;

public interface TransactionRepository {
    Mono<Transaction> save(Transaction transaction);
    Mono<Long> count();
}