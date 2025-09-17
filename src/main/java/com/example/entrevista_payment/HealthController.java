package com.example.entrevista_payment;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final TransactionRepository repository;

    @GetMapping
    public Mono<String> health() {
        return Mono.just("Payment Processor is running ✅");
    }

    @GetMapping("/transactions/count")
    public Mono<Long> getTransactionCount() {
        return repository.findAll()
                .count();
    }
}