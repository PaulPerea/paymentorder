package com.example.entrevista_payment.domain.port.in;

import reactor.core.publisher.Mono;

public interface GetTransactionCountUseCase {
    Mono<Long> getTransactionCount();
}