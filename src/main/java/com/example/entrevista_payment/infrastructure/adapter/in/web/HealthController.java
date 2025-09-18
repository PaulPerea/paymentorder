package com.example.entrevista_payment.infrastructure.adapter.in.web;

import com.example.entrevista_payment.domain.port.in.GetTransactionCountUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final GetTransactionCountUseCase getTransactionCountUseCase;

    @GetMapping
    public Mono<String> health() {
        return Mono.just("Payment Processor is running ✅");
    }

    @GetMapping("/transactions/count")
    public Mono<Long> getTransactionCount() {
        return getTransactionCountUseCase.getTransactionCount();
    }
}