package com.example.entrevista_payment;

import com.azure.storage.queue.QueueAsyncClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
public class AzureQueueService {

    private final QueueAsyncClient queueClient;
    private final ObjectMapper objectMapper;

    public AzureQueueService(@Value("${azure.storage.connection-string}") String connectionString,
                             @Value("${azure.storage.queue-name}") String queueName,
                             ObjectMapper objectMapper) {
        this.queueClient = new QueueClientBuilder()
                .connectionString(connectionString)
                .queueName(queueName)
                .buildAsyncClient();
        this.objectMapper = objectMapper;

        // Crear la cola si no existe
        queueClient.create().subscribe(
                result -> log.info("Queue created: {}", queueName),
                error -> {
                    if (!error.getMessage().contains("QueueAlreadyExists")) {
                        log.error("Error creating queue: {}", error.getMessage());
                    }
                }
        );
    }

    public Flux<Order> pollOrders() {
        return Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick -> receiveMessages())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .doOnNext(order -> log.debug("Received order: {}", order.orderId()));
    }

    private Mono<Optional<Order>> receiveMessages() {
        return queueClient.receiveMessage()
                .map(this::parseOrder)
                .flatMap(orderOpt -> {
                    if (orderOpt.isPresent()) {
                        return queueClient.deleteMessage(orderOpt.get().toString(), "")
                                .then(Mono.just(orderOpt));
                    }
                    return Mono.just(Optional.<Order>empty());
                })
                .onErrorReturn(Optional.empty());
    }

    private Optional<Order> parseOrder(QueueMessageItem message) {
        try {
            Order order = objectMapper.readValue(message.getBody().toString(), Order.class);
            log.debug("Parsed order: {}", order);
            return Optional.of(order);
        } catch (JsonProcessingException e) {
            log.error("Error parsing order from queue message: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
