package com.example.entrevista_payment;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "azure")
public class AzureConfig {

    private Cosmos cosmos = new Cosmos();
    private Storage storage = new Storage();

    @Data
    public static class Cosmos {
        private String endpoint;
        private String key;
        private String database;
        private String container;
    }

    @Data
    public static class Storage {
        private String connectionString;
        private String queueName;
    }
}