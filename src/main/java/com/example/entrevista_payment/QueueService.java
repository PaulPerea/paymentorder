package com.example.entrevista_payment;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
public class QueueService {

    private final QueueClient queueClient;
    private final ObjectMapper objectMapper;

    public QueueService(@Value("${azure.storage.queue.connection-string}") String connectionString,
                        @Value("${azure.storage.queue.queue-name}") String queueName,
                        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.queueClient = new QueueClientBuilder()
                .connectionString(connectionString)
                .queueName(queueName)
                .buildClient();

        // Crear la cola si no existe - en hilo bloqueante
        try {
            queueClient.create();
            log.info("Cola '{}' creada", queueName);
        } catch (Exception e) {
            log.info("Cola '{}' ya existe", queueName);
        }
    }

    /**
     * 🔧 CORREGIDO: Obtiene mensajes usando scheduler para operaciones blocking
     */
    public Flux<QueueMessageItem> receiveMessages() {
        return Mono.fromCallable(() -> {
                    // Esta operación blocking se ejecuta en un hilo apropiado
                    var messages = queueClient.receiveMessages(10);
                    return messages.stream().toList();
                })
                .subscribeOn(Schedulers.boundedElastic()) // 🔑 CLAVE: usar scheduler apropiado
                .flatMapMany(Flux::fromIterable)
                .doOnNext(msg -> log.debug("Mensaje recibido: {}", msg.getMessageId()))
                .onErrorResume(e -> {
                    log.error("Error recibiendo mensajes de cola: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    /**
     * Convierte mensaje a Order usando Optional - SIN CAMBIOS (ya está bien)
     */
    public Optional<Order> parseOrder(String messageText) {
        return Optional.ofNullable(messageText)
                .flatMap(text -> {
                    try {
                        return Optional.of(objectMapper.readValue(text, Order.class));
                    } catch (Exception e) {
                        log.error("Error parseando orden: {}", e.getMessage());
                        return Optional.empty();
                    }
                });
    }

    /**
     * 🔧 CORREGIDO: Elimina mensaje usando scheduler para operaciones blocking
     */
    public Mono<Void> deleteMessage(QueueMessageItem message) {
        return Mono.fromRunnable(() -> {
                    // Esta operación blocking se ejecuta en un hilo apropiado
                    queueClient.deleteMessage(message.getMessageId(), message.getPopReceipt());
                    log.debug("Mensaje {} eliminado de la cola", message.getMessageId());
                })
                .subscribeOn(Schedulers.boundedElastic()) // 🔑 CLAVE: usar scheduler apropiado
                .then()
                .onErrorResume(e -> {
                    log.error("Error eliminando mensaje {}: {}", message.getMessageId(), e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * 🆕 NUEVO: Método para enviar mensajes de prueba (opcional)
     */
    public Mono<Void> sendTestMessage(Order order) {
        return Mono.fromRunnable(() -> {
                    try {
                        String orderJson = objectMapper.writeValueAsString(order);
                        queueClient.sendMessage(orderJson);
                        log.info("Mensaje de prueba enviado: {}", order.getOrderId());
                    } catch (Exception e) {
                        log.error("Error enviando mensaje de prueba: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .onErrorResume(e -> {
                    log.error("Error enviando mensaje de prueba: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}