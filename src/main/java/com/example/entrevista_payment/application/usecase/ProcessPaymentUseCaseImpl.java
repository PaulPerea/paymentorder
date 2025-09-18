package com.example.entrevista_payment.application.usecase;

import com.example.entrevista_payment.domain.model.AuditLog;
import com.example.entrevista_payment.domain.model.Order;
import com.example.entrevista_payment.domain.model.Transaction;
import com.example.entrevista_payment.domain.port.in.ProcessPaymentUseCase;
import com.example.entrevista_payment.domain.port.out.AuditRepository;
import com.example.entrevista_payment.domain.port.out.TransactionRepository;
import com.example.entrevista_payment.domain.service.PaymentDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCaseImpl implements ProcessPaymentUseCase {

    private final TransactionRepository transactionRepository;
    private final AuditRepository auditRepository;
    private final PaymentDomainService paymentDomainService;

    @Override
    public Mono<Transaction> processPayment(Order order) {
        Instant startTime = Instant.now();

        return Mono.fromCallable(() -> paymentDomainService.createTransactionFromOrder(order))
                .flatMap(transactionRepository::save)
                .flatMap(transaction -> createAudit(order, transaction, startTime)
                        .then(Mono.just(transaction)))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .doBeforeRetry(signal ->
                                log.warn("Retry attempt #{} for order {}",
                                        signal.totalRetries() + 1, order.getOrderId())))
                .doOnSuccess(t -> log.info("Transaccion completada: {}", t.getId()))
                .doOnError(e -> log.error("Error en transaccion {}: {}",
                        order.getOrderId(), e.getMessage()));
    }

    private Mono<Void> createAudit(Order order, Transaction transaction, Instant startTime) {
        return Mono.fromCallable(() ->
                        AuditLog.createSuccessAudit(order, transaction, startTime))
                .flatMap(auditRepository::save)
                .onErrorResume(e -> {
                    log.warn("No pudo crear auditoria: {}",
                            transaction.getId());
                    return Mono.empty();
                });
    }
}