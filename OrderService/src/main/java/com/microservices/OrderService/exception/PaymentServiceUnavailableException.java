package com.microservices.OrderService.exception;

public class PaymentServiceUnavailableException extends RuntimeException{
    public PaymentServiceUnavailableException(String message) {
        super(message);
    }

    public PaymentServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
