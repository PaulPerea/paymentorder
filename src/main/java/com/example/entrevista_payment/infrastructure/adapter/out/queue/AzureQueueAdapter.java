package com.example.entrevista_payment.infrastructure.adapter.out.queue;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import com.example.entrevista_payment.domain.model.Order;
import com.example.entrevista_payment.domain.port.out.OrderQueuePort;
import com.example.entrevista_payment.infrastructure.adapter.out.persistence.entity.OrderDto;
import com.example.entrevista_payment.infrastructure.mapper.OrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class AzureQueueAdapter implements OrderQueuePort {

    private final QueueClient queueClient;
    private final ObjectMapper objectMapper;
    private final OrderMapper orderMapper;

    public AzureQueueAdapter(
            @Value("${azure.storage.queue.connection-string}") String connectionString,
            @Value("${azure.storage.queue.queue-name}") String queueName,
            ObjectMapper objectMapper,
            OrderMapper orderMapper) {

        this.objectMapper = objectMapper;
        this.orderMapper = orderMapper;

        this.queueClient = new QueueClientBuilder()
                .connectionString(connectionString)
                .queueName(queueName)
                .buildClient();

        try {
            queueClient.create();
            log.info("Queue '{}' created", queueName);
        } catch (Exception e) {
            log.info("Queue '{}' already exists", queueName);
        }
    }

    @Override
    public Flux<QueueMessage> receiveMessages() {
        return Mono.fromCallable(() -> {
                    var messages = queueClient.receiveMessages(10);
                    return messages.stream().toList();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(this::toQueueMessage)
                .filter(msg -> msg.getOrder() != null)
                .doOnNext(msg -> log.debug("Message recibido: {}", msg.getMessageId()))
                .onErrorResume(e -> {
                    log.error("Error recibiendo mensaje de queue: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    @Override
    public Mono<Void> deleteMessage(QueueMessage message) {
        return Mono.fromRunnable(() -> {
                    AzureQueueMessage azureMsg = (AzureQueueMessage) message;
                    queueClient.deleteMessage(azureMsg.getMessageId(), azureMsg.getPopReceipt());
                    log.debug("Message {} eliminar msm cola", message.getMessageId());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .onErrorResume(e -> {
                    log.error("Error eliminando msm {}: {}", message.getMessageId(), e.getMessage());
                    return Mono.empty();
                });
    }

    private QueueMessage toQueueMessage(QueueMessageItem item) {
        Order order = parseOrder(item.getMessageText());
        return new AzureQueueMessage(item.getMessageId(), item.getPopReceipt(), order);
    }

    private Order parseOrder(String messageText) {
        try {
            OrderDto dto = objectMapper.readValue(messageText, OrderDto.class);
            return orderMapper.toDomain(dto);
        } catch (Exception e) {
            log.error("Error  order: {}", e.getMessage());
            return null;
        }
    }

    @RequiredArgsConstructor
    private static class AzureQueueMessage implements QueueMessage {
        private final String messageId;
        private final String popReceipt;
        private final Order order;

        @Override
        public String getMessageId() { return messageId; }

        @Override
        public String getPopReceipt() { return popReceipt; }

        @Override
        public Order getOrder() { return order; }
    }
}