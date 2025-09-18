package com.example.entrevista_payment.domain.service;

import com.example.entrevista_payment.domain.model.Order;
import com.example.entrevista_payment.domain.model.Transaction;
import com.example.entrevista_payment.domain.exception.InvalidOrderException;

public class PaymentDomainService {

    public void validateOrderForPayment(Order order) {
        if (order == null) {
            throw new InvalidOrderException("Order no puede ser null");
        }

        if (order.getTotalAmount().isNegativeOrZero()) {
            throw new InvalidOrderException("Order cantidad total mayor a 0");
        }

        if (order.getItems().isEmpty()) {
            throw new InvalidOrderException("Order tiene que tener items");
        }
    }

    public Transaction createTransactionFromOrder(Order order) {
        validateOrderForPayment(order);
        return Transaction.createFromOrder(order);
    }
}
