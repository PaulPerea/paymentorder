package com.example.entrevista_payment.infrastructure.adapter.in.queue;

import com.example.entrevista_payment.domain.port.in.ProcessPaymentUseCase;
import com.example.entrevista_payment.domain.port.out.OrderQueuePort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueMessageProcessor {

    private final OrderQueuePort orderQueuePort;
    private final ProcessPaymentUseCase processPaymentUseCase;

    @Value("${payment.processor.polling-interval-ms}")
    private long pollingInterval;

    @PostConstruct
    public void startProcessing() {
        log.info("Payment - procesando...");

        Flux.interval(Duration.ofMillis(pollingInterval))
                .flatMap(tick -> processQueueMessages())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        result -> {},
                        error -> log.error("Error en proceso: {}", error.getMessage()),
                        () -> log.info("Proceso stop")
                );
    }

    private Flux<Void> processQueueMessages() {
        return orderQueuePort.receiveMessages()
                .flatMap(message -> {
                    log.info("Procesando message de cola: {}", message.getMessageId());

                    return processPaymentUseCase.processPayment(message.getOrder())
                            .then(orderQueuePort.deleteMessage(message))
                            .onErrorResume(e -> {
                                log.error("Error procesando order, msm en queue: {}",
                                        e.getMessage());
                                return Flux.empty().then();
                            });
                })
                .doOnComplete(() -> log.debug("MSM batch en queu"));
    }
}
