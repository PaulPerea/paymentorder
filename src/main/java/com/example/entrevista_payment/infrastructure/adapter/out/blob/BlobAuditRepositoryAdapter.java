package com.example.entrevista_payment.infrastructure.adapter.out.blob;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.example.entrevista_payment.domain.model.AuditLog;
import com.example.entrevista_payment.domain.port.out.AuditRepository;
import com.example.entrevista_payment.infrastructure.mapper.AuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@ConditionalOnProperty(value = "audit.enabled", havingValue = "true", matchIfMissing = false)
public class BlobAuditRepositoryAdapter implements AuditRepository {

    private final BlobContainerAsyncClient containerClient;
    private final ObjectMapper objectMapper;
    private final AuditLogMapper auditLogMapper;
    private final boolean auditEnabled;

    private static final String CONTAINER_NAME = "audits";

    public BlobAuditRepositoryAdapter(
            @Value("${azure.storage.blob.connection-string}") String connectionString,
            @Value("${audit.enabled:false}") boolean auditEnabled,
            ObjectMapper objectMapper,
            AuditLogMapper auditLogMapper) {

        this.auditEnabled = auditEnabled;
        this.objectMapper = objectMapper;
        this.auditLogMapper = auditLogMapper;

        if (auditEnabled) {
            BlobServiceAsyncClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildAsyncClient();

            this.containerClient = blobServiceClient.getBlobContainerAsyncClient(CONTAINER_NAME);
            createContainerIfNotExists();
        } else {
            this.containerClient = null;
        }
    }

    @Override
    public Mono<Void> save(AuditLog auditLog) {
        if (!auditEnabled) {
            return Mono.empty();
        }

        return Mono.fromCallable(() -> auditLogMapper.toDto(auditLog))
                .flatMap(dto -> saveToBlobStorage(dto, auditLog))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> log.debug("Auditoria guardada: {}",
                        auditLog.getTransaction().getId()))
                .doOnError(e -> log.error("Error en guardad la auditoria: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.warn("No se realizo la transaccion: {}",
                            auditLog.getTransaction().getId());
                    return Mono.empty();
                });
    }

    private Mono<Void> saveToBlobStorage(Object dto, AuditLog auditLog) {
        return Mono.fromCallable(() -> {
                    String blobPath = generateBlobPath(auditLog);
                    String jsonContent = objectMapper.writeValueAsString(dto);

                    BinaryData binaryData = BinaryData.fromString(jsonContent);
                    BlobAsyncClient blobClient = containerClient.getBlobAsyncClient(blobPath);

                    return blobClient.upload(binaryData, true);
                })
                .flatMap(uploadMono -> uploadMono)
                .then()
                .doOnSuccess(v -> log.info("Auditoria blog: {}", generateBlobPath(auditLog)));
    }

    private String generateBlobPath(AuditLog auditLog) {
        LocalDate date = auditLog.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
        String datePath = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String filename = String.format("transaction-%s.json",
                auditLog.getTransaction().getId().getValue());
        return String.format("%s/%s", datePath, filename);
    }

    private void createContainerIfNotExists() {
        containerClient.createIfNotExists()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        response -> log.debug("Contenedor '{}' verificacion-creada", CONTAINER_NAME),
                        error -> log.warn("No se puede crear contenedor '{}': {}",
                                CONTAINER_NAME, error.getMessage())
                );
    }
}