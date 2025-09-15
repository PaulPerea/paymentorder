package com.example.entrevista_payment;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosAsyncContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CosmosConfig {

    @Value("${azure.cosmos.endpoint}")
    private String endpoint;

    @Value("${azure.cosmos.key}")
    private String key;

    @Value("${azure.cosmos.database}")
    private String databaseName;

    @Value("${azure.cosmos.container}")
    private String containerName;

    @Bean
    public CosmosAsyncClient cosmosAsyncClient() {
        return new CosmosClientBuilder()
                .endpoint(endpoint)
                .key(key)
                .buildAsyncClient();
    }

    @Bean
    public CosmosAsyncDatabase cosmosAsyncDatabase(CosmosAsyncClient cosmosAsyncClient) {
        return cosmosAsyncClient.getDatabase(databaseName);
    }

    @Bean
    public CosmosAsyncContainer cosmosAsyncContainer(CosmosAsyncDatabase cosmosAsyncDatabase) {
        return cosmosAsyncDatabase.getContainer(containerName);
    }
}