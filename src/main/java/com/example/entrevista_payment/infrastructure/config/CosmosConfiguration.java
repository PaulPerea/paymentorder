package com.example.entrevista_payment.infrastructure.config;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.repository.config.EnableReactiveCosmosRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableReactiveCosmosRepositories(
        basePackages = "com.example.entrevista_payment.infrastructure.adapter.out.persistence.cosmos"
)
public class CosmosConfiguration extends AbstractCosmosConfiguration {

    @Value("${azure.cosmos.endpoint}")
    private String endpoint;

    @Value("${azure.cosmos.key}")
    private String key;

    @Value("${azure.cosmos.database}")
    private String database;

    @Bean
    public CosmosClientBuilder cosmosClientBuilder() {
        return new CosmosClientBuilder()
                .endpoint(endpoint)
                .key(key);
    }

    @Override
    protected String getDatabaseName() {
        return database;
    }
}