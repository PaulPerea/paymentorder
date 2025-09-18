package com.example.entrevista_payment;

import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(value = "audit.enabled", havingValue = "true")
public class BlobAuditService {

    private final BlobContainerAsyncClient containerClient;
    private final ObjectMapper objectMapper;
    private final boolean auditEnabled;

    private static final String CONTAINER_NAME = "audits";
    private static final String EVENT_TYPE = "PAYMENT_PROCESSED";

    public BlobAuditService(
            @Value("${azure.storage.blob.connection-string}") String connectionString,
            @Value("${audit.enabled:false}") boolean auditEnabled,
            ObjectMapper objectMapper) {

        this.auditEnabled = auditEnabled;
        this.objectMapper = objectMapper;

        if (auditEnabled) {
            BlobServiceAsyncClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildAsyncClient();

            this.containerClient = blobServiceClient.getBlobContainerAsyncClient(CONTAINER_NAME);

            // Crear container si no existe
            createContainerIfNotExists();
            log.info("🗂️ BlobAuditService inicializado - Auditoría HABILITADA");
        } else {
            this.containerClient = null;
            log.info("🚫 BlobAuditService inicializado - Auditoría DESHABILITADA");
        }
    }

    /**
     * Crea una auditoría exitosa usando streams y Optional
     */
    public Mono<Void> auditSuccessfulTransaction(Order order, Transaction transaction, Instant startTime) {
        if (!auditEnabled) {
            return Mono.empty();
        }

        return Mono.fromCallable(() -> buildAuditLog(order, transaction, startTime))
                .flatMap(this::validateAuditLog)
                .flatMap(this::saveAuditToBlob)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> log.debug("✅ Auditoría guardada para transacción: {}", transaction.getId()))
                .doOnError(e -> log.error("❌ Error guardando auditoría para transacción {}: {}",
                        transaction.getId(), e.getMessage()))
                .onErrorResume(e -> {
                    log.warn("⚠️ No se pudo realizar la auditoría para transacción: {}", transaction.getId());
                    return Mono.empty();
                });
    }

    /**
     * Construye el log de auditoría usando Optional para validaciones
     */
    private AuditLog buildAuditLog(Order order, Transaction transaction, Instant startTime) {
        Instant now = Instant.now();

        return AuditLog.builder()
                .auditId(UUID.randomUUID().toString())
                .timestamp(now)
                .eventType(EVENT_TYPE)
                .status("SUCCESS")
                .order(order)
                .transaction(transaction)
                .processingTime(Duration.between(startTime, now))
                .error(null)
                .build();
    }

    /**
     * Valida el log de auditoría usando Optional y streams
     */
    private Mono<AuditLog> validateAuditLog(AuditLog auditLog) {
        return Mono.fromCallable(() -> {
            // Validar campos requeridos usando Optional
            Optional.ofNullable(auditLog.getOrder())
                    .filter(order -> Optional.ofNullable(order.getOrderId()).isPresent())
                    .orElseThrow(() -> new IllegalArgumentException("Orden inválida para auditoría"));

            Optional.ofNullable(auditLog.getTransaction())
                    .filter(tx -> Optional.ofNullable(tx.getId()).isPresent())
                    .orElseThrow(() -> new IllegalArgumentException("Transacción inválida para auditoría"));

            // Validar que la orden tenga items válidos usando streams
            boolean itemsValid = Optional.ofNullable(auditLog.getOrder().getItems())
                    .map(items -> items.stream()
                            .allMatch(item ->
                                    Optional.ofNullable(item.getProductId()).isPresent() &&
                                            Optional.ofNullable(item.getQuantity()).filter(qty -> qty > 0).isPresent()
                            ))
                    .orElse(false);

            if (!itemsValid) {
                throw new IllegalArgumentException("Items de orden inválidos para auditoría");
            }

            log.debug("Auditoría validada para transacción: {}", auditLog.getTransaction().getId());
            return auditLog;
        });
    }

    /**
     * Guarda la auditoría en Blob Storage usando BinaryData (método más simple)
     */
    private Mono<Void> saveAuditToBlob(AuditLog auditLog) {
        return Mono.fromCallable(() -> {
                    try {
                        String blobPath = generateBlobPath(auditLog);
                        String jsonContent = objectMapper.writeValueAsString(auditLog);

                        log.debug("📄 JSON generado para auditoría: {}", jsonContent);

                        // Usar BinaryData - es la forma más simple y estable
                        BinaryData binaryData = BinaryData.fromString(jsonContent);
                        BlobAsyncClient blobClient = containerClient.getBlobAsyncClient(blobPath);

                        // Este método SÍ existe y funciona correctamente
                        return blobClient.upload(binaryData, true); // true = overwrite

                    } catch (Exception e) {
                        log.error("💥 Error detallado en serialización: {}", e.getMessage(), e);
                        throw new RuntimeException("Error serializando auditoría: " + e.getMessage(), e);
                    }
                })
                .flatMap(uploadMono -> uploadMono) // uploadMono es Mono<BlockBlobItem>
                .then() // Convertir a Mono<Void>
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> log.info("✅ Auditoría guardada en blob: {}", generateBlobPath(auditLog)))
                .doOnError(e -> log.error("💥 Error completo guardando blob: {}", e.getMessage(), e));
    }

    /**
     * Genera la ruta del blob: /audits/2025/09/16/transaction-{id}.json
     */
    private String generateBlobPath(AuditLog auditLog) {
        LocalDate date = auditLog.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
        String datePath = date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String filename = String.format("transaction-%s.json", auditLog.getTransaction().getId());

        return String.format("%s/%s", datePath, filename);
    }

    /**
     * Crea el container de auditorías si no existe
     */
    private void createContainerIfNotExists() {
        containerClient.createIfNotExists()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        response -> log.debug("Container '{}' verificado/creado", CONTAINER_NAME),
                        error -> log.warn("No se pudo crear container '{}': {}", CONTAINER_NAME, error.getMessage())
                );
    }

    /**
     * Verifica si la auditoría está habilitada
     */
    public boolean isAuditEnabled() {
        return auditEnabled;
    }
}