package com.microservices.OrderService.exception;

public class UserServiceErrorException extends RuntimeException {
    public UserServiceErrorException(String message) {
        super(message);
    }
    
    public UserServiceErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
