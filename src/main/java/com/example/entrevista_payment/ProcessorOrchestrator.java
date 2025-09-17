package com.example.entrevista_payment;

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
public class ProcessorOrchestrator {

    private final QueueService queueService;
    private final PaymentService paymentService;

    @Value("${payment.processor.polling-interval-ms}")
    private long pollingInterval;

    @PostConstruct
    public void startProcessing() {
        log.info("🚀 Iniciando Payment Processor...");

        // Proceso infinito que revisa la cola periódicamente
        Flux.interval(Duration.ofMillis(pollingInterval))
                .flatMap(tick -> processQueueMessages())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        result -> {}, // Ya se loguea en los servicios
                        error -> log.error("Error en el procesador: {}", error.getMessage()),
                        () -> log.info("Procesador detenido")
                );
    }

    private Flux<Void> processQueueMessages() {
        return queueService.receiveMessages()
                .flatMap(message -> {
                    log.info("📨 Procesando mensaje: {}", message.getMessageId());

                    return queueService.parseOrder(message.getMessageText())
                            .map(order ->
                                    paymentService.processPayment(order)
                                            .then(queueService.deleteMessage(message))
                                            .onErrorResume(e -> {
                                                log.error("Error procesando orden, mensaje permanece en cola: {}",
                                                        e.getMessage());
                                                return Flux.empty().then();
                                            })
                            )
                            .orElse(Flux.empty().then());
                })
                .doOnComplete(() -> log.debug("Batch de mensajes procesado"));
    }
}