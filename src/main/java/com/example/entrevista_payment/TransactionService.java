package com.example.entrevista_payment;

import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final CosmosAsyncContainer cosmosContainer;

    public Mono<Transaction> processOrder(Order order) {
        return validateOrder(order)
                .flatMap(this::processPayment)
                .flatMap(this::saveTransaction)
                .doOnNext(transaction -> log.info("Transaction processed: {} for order: {}",
                        transaction.id(), transaction.orderId()))
                .doOnError(error -> log.error("Error processing order {}: {}",
                        order.orderId(), error.getMessage()));
    }

    private Mono<Order> validateOrder(Order order) {
        return Mono.fromCallable(() -> {
            // Validaciones usando Optional y Streams
            Optional.of(order)
                    .filter(o -> o.totalAmount().compareTo(BigDecimal.ZERO) > 0)
                    .orElseThrow(() -> new IllegalArgumentException("Total amount must be positive"));

            boolean hasValidItems = order.items().stream()
                    .allMatch(item -> item.quantity() > 0 &&
                            Optional.ofNullable(item.productId()).isPresent());

            if (!hasValidItems) {
                throw new IllegalArgumentException("Invalid items in order");
            }

            // Validar que el total calculado coincida con el total de la orden
            BigDecimal calculatedTotal = order.items().stream()
                    .map(item -> BigDecimal.valueOf(item.quantity() * 10.0)) // Precio ejemplo
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.debug("Order validation completed for: {}", order.orderId());
            return order;
        });
    }

    private Mono<Transaction> processPayment(Order order) {
        return Mono.fromCallable(() -> {
            // Simulación del procesamiento de pago
            boolean paymentSuccessful = order.totalAmount().compareTo(BigDecimal.valueOf(1000)) <= 0;

            Transaction.TransactionStatus status = paymentSuccessful ?
                    Transaction.TransactionStatus.PROCESSED :
                    Transaction.TransactionStatus.FAILED;

            log.debug("Payment processed for order {}: {}", order.orderId(), status);
            return Transaction.from(order, status);
        });
    }

    private Mono<Transaction> saveTransaction(Transaction transaction) {
        return cosmosContainer
                .createItem(transaction)
                .map(CosmosItemResponse::getItem)
                .cast(Transaction.class)
                .doOnNext(saved -> log.debug("Transaction saved: {}", saved.id()))
                .onErrorMap(error -> {
                    log.error("Error saving transaction: {}", error.getMessage());
                    return new RuntimeException("Failed to save transaction", error);
                });
    }

    public Mono<Transaction> getTransaction(String transactionId, String customerId) {
        return cosmosContainer
                .readItem(transactionId, new PartitionKey(customerId), Transaction.class)
                .map(CosmosItemResponse::getItem)
                .doOnNext(transaction -> log.debug("Retrieved transaction: {}", transaction.id()));
    }
}