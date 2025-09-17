package com.example.entrevista_payment;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.GatewayConnectionConfig;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.repository.config.EnableReactiveCosmosRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.Duration;

@Configuration
@Slf4j
@EnableReactiveCosmosRepositories(basePackages = "com.example.entrevista_payment")
public class CosmosConfiguration extends AbstractCosmosConfiguration {

    @Value("${azure.cosmos.endpoint}")
    private String endpoint;

    @Value("${azure.cosmos.key}")
    private String key;

    @Value("${azure.cosmos.database}")
    private String database;

    @Bean
    public CosmosClientBuilder cosmosClientBuilder() {
        log.info("🔧 Configurando Cosmos DB Client con certificados SSL...");
        log.info("📍 Endpoint: {}", endpoint);
        log.info("🖥️ OS: {}", System.getProperty("os.name"));
        log.info("☕ Java Home: {}", System.getProperty("java.home"));

        // Configurar SSL usando el certificado en cacerts
        if (isEmulatorEnvironment()) {
            configurarSSLConCertificado();
        }

        // Configuración específica para emulador
        GatewayConnectionConfig gatewayConfig = new GatewayConnectionConfig();
        gatewayConfig.setMaxConnectionPoolSize(100);
        gatewayConfig.setIdleConnectionTimeout(Duration.ofSeconds(10));

        CosmosClientBuilder builder = new CosmosClientBuilder()
                .endpoint(endpoint)
                .key(key)
                .gatewayMode(gatewayConfig)
                .endpointDiscoveryEnabled(false)
                .connectionSharingAcrossClientsEnabled(false)
                .contentResponseOnWriteEnabled(true);

        log.info("✅ Cosmos DB Client configurado con certificados SSL");
        return builder;
    }

    @Override
    protected String getDatabaseName() {
        return database;
    }

    private boolean isEmulatorEnvironment() {
        return endpoint.contains("localhost") ||
                endpoint.contains("127.0.0.1") ||
                endpoint.contains(":8081");
    }

    /**
     * Configuración SSL usando el certificado del emulador en cacerts
     * En lugar de deshabilitar SSL, usa el certificado correcto
     */
    private void configurarSSLConCertificado() {
        try {
            log.info("🔒 Configurando SSL usando certificado en cacerts...");

            // Obtener la ruta del truststore Java (cacerts)
            String javaHome = System.getProperty("java.home");
            String cacertsPath = javaHome + "/lib/security/cacerts";
            String cacertsPassword = "changeit";

            log.info("📂 Usando truststore: {}", cacertsPath);

            // Verificar que el archivo cacerts existe
            java.io.File cacertsFile = new java.io.File(cacertsPath);
            if (!cacertsFile.exists()) {
                log.error("❌ No se encuentra cacerts en: {}", cacertsPath);
                throw new RuntimeException("cacerts no encontrado");
            }

            // Cargar el KeyStore de Java (cacerts)
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(cacertsPath)) {
                trustStore.load(fis, cacertsPassword.toCharArray());
            }

            // Verificar que el certificado del emulador está en cacerts
            if (trustStore.containsAlias("cosmosdb")) {
                log.info("✅ Certificado 'cosmosdb' encontrado en cacerts");
            } else {
                log.warn("⚠️ Certificado 'cosmosdb' NO encontrado en cacerts");
                log.warn("💡 Ejecuta: keytool -import -trustcacerts -keystore \"{}\" -storepass changeit -noprompt -alias cosmosdb -file emulator.crt", cacertsPath);
            }

            // Crear TrustManagerFactory usando el cacerts
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            // Crear SSLContext usando el TrustManagerFactory
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());

            // Configurar el SSLContext como default
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // Configurar hostname verifier para localhost/127.0.0.1
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> {
                log.debug("🏷️ Verificando hostname: {}", hostname);

                // Permitir localhost, 127.0.0.1 y el CN del certificado
                boolean isValid = "localhost".equalsIgnoreCase(hostname) ||
                        "127.0.0.1".equals(hostname) ||
                        hostname.contains("cosmosdb") ||
                        hostname.contains("emulator");

                if (isValid) {
                    log.debug("✅ Hostname válido: {}", hostname);
                } else {
                    log.warn("❌ Hostname NO válido: {}", hostname);
                }

                return isValid;
            });

            // Configurar propiedades del sistema para Azure Cosmos SDK
            System.setProperty("javax.net.ssl.trustStore", cacertsPath);
            System.setProperty("javax.net.ssl.trustStorePassword", cacertsPassword);
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");

            // Configuraciones adicionales para el SDK de Azure
            System.setProperty("java.net.useSystemProxies", "false");
            System.setProperty("com.azure.cosmos.directModeProtocol", "Tcp");
            System.setProperty("azure.cosmos.tcp.connectionTimeout", "PT5S");
            System.setProperty("azure.cosmos.tcp.networkRequestTimeout", "PT60S");

            // IMPORTANTE: NO deshabilitar la verificación SSL
            // System.setProperty("com.azure.cosmos.emulator.disable.ssl.verification", "true"); // ❌ NO hacer esto

            log.info("✅ SSL configurado correctamente usando certificado en cacerts");
            log.info("🔐 TrustStore: {}", cacertsPath);
            log.info("🏷️ Hostname verification habilitado para localhost/127.0.0.1");

        } catch (Exception e) {
            log.error("❌ Error configurando SSL con certificados: {}", e.getMessage(), e);
            log.error("💡 Asegúrate de que el certificado del emulador esté importado en cacerts");
            log.error("💡 Comando: keytool -import -trustcacerts -keystore \"{}\" -storepass changeit -noprompt -alias cosmosdb -file emulator.crt",
                    System.getProperty("java.home") + "/lib/security/cacerts");
            throw new RuntimeException("Error en configuración SSL con certificados", e);
        }
    }
}