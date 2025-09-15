package com.example.entrevista_payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final TransactionService transactionService;

    @GetMapping(value = "/transactions/{transactionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Transaction> getTransaction(
            @PathVariable String transactionId,
            @RequestParam String customerId) {
        return transactionService.getTransaction(transactionId, customerId);
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> health() {
        return Mono.just("{\"status\":\"UP\"}");
    }
}