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
    private final Optional<BlobAuditService> blobAuditService; // Optional para manejar cuando está deshabilitado

    // Procesa una orden y crea una transacción con auditoría
    public Mono<Transaction> processPayment(Order order) {
        Instant startTime = Instant.now(); // Para calcular tiempo de procesamiento

        return validateOrder(order)
                .flatMap(this::createTransaction)
                .flatMap(this::saveTransaction)
                .flatMap(transaction -> auditSuccessfulTransaction(order, transaction, startTime)
                        .then(Mono.just(transaction))) // Continúa con la transacción después de auditar
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .doBeforeRetry(signal ->
                                log.warn("Reintento #{} para orden {}",
                                        signal.totalRetries() + 1, order.getOrderId())))
                .doOnSuccess(t -> log.info("✅ Transacción completada: {}", t.getId()))
                .doOnError(e -> {
                    log.error("❌ Error procesando orden {}: {}", order.getOrderId(), e.getMessage());
                    // En caso de error, solo logueamos que no se pudo auditar
                    blobAuditService.ifPresent(auditService ->
                            log.warn("⚠️ No se pudo realizar la auditoría para orden fallida: {}", order.getOrderId())
                    );
                });
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
                                    .isPresent() &&
                                    Optional.ofNullable(item.getProductId())
                                            .filter(productId -> !productId.trim().isEmpty())
                                            .isPresent());

            if (!itemsValidos) {
                throw new IllegalArgumentException("Items con datos inválidos");
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

    // Audita transacción exitosa en Blob Storage
    private Mono<Void> auditSuccessfulTransaction(Order order, Transaction transaction, Instant startTime) {
        return blobAuditService
                .map(auditService -> auditService.auditSuccessfulTransaction(order, transaction, startTime))
                .orElse(Mono.empty()) // Si no hay servicio de auditoría, no hace nada
                .doOnSubscribe(s -> log.debug("Iniciando auditoría para transacción: {}", transaction.getId()));
    }
}