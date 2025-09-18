package com.example.entrevista_payment.infrastructure.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private String orderId;
    private String customerId;
    private List<ItemDto> items;
    private Double totalAmount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDto {
        private String productId;
        private Integer quantity;
    }
}
