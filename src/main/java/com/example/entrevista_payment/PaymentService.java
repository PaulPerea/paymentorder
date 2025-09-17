package com.example.entrevista_payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;

    // Procesa una orden y crea una transacción
    public Mono<Transaction> processPayment(Order order) {
        return validateOrder(order)
                .flatMap(this::createTransaction)
                .flatMap(this::saveTransaction)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .doBeforeRetry(signal ->
                                log.warn("Reintento #{} para orden {}",
                                        signal.totalRetries() + 1, order.getOrderId())))
                .doOnSuccess(t -> log.info("✅ Transacción completada: {}", t.getId()))
                .doOnError(e -> log.error("❌ Error procesando orden {}: {}",
                        order.getOrderId(), e.getMessage()));
    }

    // Valida la orden usando Optional y streams
    private Mono<Order> validateOrder(Order order) {
        return Mono.fromCallable(() -> {
            Optional.ofNullable(order.getOrderId())
                    .filter(id -> !id.isEmpty())
                    .orElseThrow(() -> new IllegalArgumentException("OrderId vacío"));

            Optional.ofNullable(order.getTotalAmount())
                    .filter(amount -> amount > 0)
                    .orElseThrow(() -> new IllegalArgumentException("Monto inválido"));

            Optional.ofNullable(order.getItems())
                    .filter(items -> !items.isEmpty())
                    .orElseThrow(() -> new IllegalArgumentException("Sin items"));

            // Validar items usando streams
            boolean itemsValidos = order.getItems().stream()
                    .allMatch(item ->
                            Optional.ofNullable(item.getQuantity())
                                    .filter(qty -> qty > 0)
                                    .isPresent());

            if (!itemsValidos) {
                throw new IllegalArgumentException("Items con cantidad inválida");
            }

            log.debug("Orden {} validada correctamente", order.getOrderId());
            return order;
        });
    }

    // Crea la transacción
    private Mono<Transaction> createTransaction(Order order) {
        return Mono.fromCallable(() -> {
            Transaction transaction = Transaction.builder()
                    .id(UUID.randomUUID().toString())
                    .orderId(order.getOrderId())
                    .customerId(order.getCustomerId())
                    .amount(order.getTotalAmount())
                    .status("COMPLETED")
                    .timestamp(Instant.now())
                    .processedAt(Instant.now())
                    .build();

            log.debug("Transacción creada para orden {}", order.getOrderId());
            return transaction;
        });
    }

    // Guarda en Cosmos DB
    private Mono<Transaction> saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction)
                .doOnSuccess(t -> log.debug("Transacción {} guardada en Cosmos DB", t.getId()));
    }
}