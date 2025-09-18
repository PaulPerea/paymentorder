package com.example.entrevista_payment.domain.exception;

public class InvalidOrderException extends PaymentProcessingException {
    public InvalidOrderException(String message) {
        super(message);
    }
}