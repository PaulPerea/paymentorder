package com.example.entrevista_payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record Order(
        @NotBlank
        @JsonProperty("order_id")
        String orderId,

        @NotBlank
        @JsonProperty("customer_id")
        String customerId,

        @NotEmpty
        @Valid
        List<Item> items,

        @NotNull
        @Positive
        @JsonProperty("total_amount")
        BigDecimal totalAmount
) {

    @Builder
    public record Item(
            @NotBlank
            @JsonProperty("product_id")
            String productId,

            @Positive
            int quantity
    ) {}
}