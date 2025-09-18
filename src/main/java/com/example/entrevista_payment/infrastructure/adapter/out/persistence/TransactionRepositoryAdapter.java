package com.example.entrevista_payment.infrastructure.adapter.out.persistence;

import com.example.entrevista_payment.domain.model.Transaction;
import com.example.entrevista_payment.domain.port.out.TransactionRepository;
import com.example.entrevista_payment.infrastructure.adapter.out.persistence.cosmos.CosmosTransactionRepository;
import com.example.entrevista_payment.infrastructure.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final CosmosTransactionRepository cosmosRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public Mono<Transaction> save(Transaction transaction) {
        return Mono.just(transaction)
                .map(transactionMapper::toEntity)
                .flatMap(cosmosRepository::save)
                .map(transactionMapper::toDomain)
                .doOnSuccess(t -> log.debug("Transaction guardado: {}", t.getId()))
                .doOnError(e -> log.error("Erro guardando transaction: {}", e.getMessage()));
    }

    @Override
    public Mono<Long> count() {
        return cosmosRepository.count();
    }
}