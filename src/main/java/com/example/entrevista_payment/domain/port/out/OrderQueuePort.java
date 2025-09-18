package com.example.entrevista_payment.domain.port.out;

import com.example.entrevista_payment.domain.model.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderQueuePort {
    Flux<QueueMessage> receiveMessages();
    Mono<Void> deleteMessage(QueueMessage message);

    interface QueueMessage {
        String getMessageId();
        String getPopReceipt();
        Order getOrder();
    }
}