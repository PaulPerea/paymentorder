package com.example.entrevista_payment.infrastructure.adapter.out.blob;

import com.example.entrevista_payment.domain.model.AuditLog;
import com.example.entrevista_payment.domain.port.out.AuditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@ConditionalOnProperty(value = "audit.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAuditRepositoryAdapter implements AuditRepository {

    public NoOpAuditRepositoryAdapter() {
        log.info("Auditoria repositorio - Audit DISABLED");
    }

    @Override
    public Mono<Void> save(AuditLog auditLog) {
        log.debug("Auditoria (disabled) para transaction: {}",
                auditLog.getTransaction().getId());
        return Mono.empty();
    }
}