package com.microservices.OrderService.exception;

public class DuplicateRequestException extends RuntimeException {
    
    private final Long existingOrderId;
    
    public DuplicateRequestException(String message, Long existingOrderId) {
        super(message);
        this.existingOrderId = existingOrderId;
    }

    public Long getExistingOrderId() {
        return existingOrderId;
    }
}
