package com.example.entrevista_payment.application.usecase;

import com.example.entrevista_payment.domain.port.in.GetTransactionCountUseCase;
import com.example.entrevista_payment.domain.port.out.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GetTransactionCountUseCaseImpl implements GetTransactionCountUseCase {

    private final TransactionRepository transactionRepository;

    @Override
    public Mono<Long> getTransactionCount() {
        return transactionRepository.count();
    }
}