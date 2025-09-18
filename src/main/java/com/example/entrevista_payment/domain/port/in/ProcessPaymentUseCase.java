package com.example.entrevista_payment.domain.port.in;

import com.example.entrevista_payment.domain.model.Transaction;
import com.example.entrevista_payment.domain.model.Order;
import reactor.core.publisher.Mono;

public interface ProcessPaymentUseCase {
    Mono<Transaction> processPayment(Order order);
}