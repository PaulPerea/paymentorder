package com.example.entrevista_payment.domain.port.out;

import com.example.entrevista_payment.domain.model.AuditLog;
import reactor.core.publisher.Mono;

public interface AuditRepository {
    Mono<Void> save(AuditLog auditLog);
}