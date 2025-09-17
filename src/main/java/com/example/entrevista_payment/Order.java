package com.example.entrevista_payment;

import lombok.Data;
import java.util.List;

// Modelo para la Orden que viene de la cola
@Data
public class Order {
    private String orderId;
    private String customerId;
    private List<Item> items;
    private Double totalAmount;
    private Integer totalItems;

    @Data
    public static class Item {
        private String productId;
        private Integer quantity;
    }
}