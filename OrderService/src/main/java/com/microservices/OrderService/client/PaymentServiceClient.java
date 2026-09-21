package com.microservices.OrderService.client;

import com.microservices.OrderService.dto.PaymentRequest;
import com.microservices.OrderService.dto.PaymentResponse;
import com.microservices.OrderService.exception.PaymentServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Client for Payment Service communication
 * Practice 11: Refactored to remove URL-building logic from methods
 * All URI patterns are defined as constants
 */
@Slf4j
@Component
public class PaymentServiceClient {

    // URI Pattern Constants - Practice 11: Centralized endpoint definitions
    private static final String PROCESS_PAYMENT_URI = "/api/payments";
    
    private final RestClient restClient;

    public PaymentServiceClient(RestClient paymentServiceRestClient) {
        this.restClient = paymentServiceRestClient;
    }

    /**
     * Process payment for an order
     * Practice 11: Clean method - no hardcoded URI patterns
     * 
     * NO RETRY annotation - Payment operations should not retry to avoid duplicate charges
     * 
     * @param orderId Order ID for payment
     * @param userId User ID making payment
     * @param amount Payment amount
     * @return true if payment successful, false otherwise
     * @throws PaymentServiceUnavailableException if service is unavailable (circuit breaker fallback)
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    public boolean processPayment(Long orderId, Long userId, BigDecimal amount) {
        log.debug("Processing payment: orderId={}, userId={}, amount={}", orderId, userId, amount);
        
        PaymentRequest request = new PaymentRequest(orderId, userId, amount);

        PaymentResponse response = restClient.post()
                .uri(PROCESS_PAYMENT_URI)
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);

        boolean success = response != null && "SUCCESS".equals(response.getStatus());
        log.debug("Payment processing result: orderId={}, success={}", orderId, success);
        
        return success;
    }

    /**
     * Fallback method for circuit breaker
     * Practice 11: Clean fallback signature
     * 
     * @param orderId Order ID that was being processed
     * @param userId User ID that was making payment
     * @param amount Amount that was being charged
     * @param t Throwable that caused the fallback
     * @return never returns, always throws exception
     */
    private boolean processPaymentFallback(Long orderId, Long userId, BigDecimal amount, Throwable t) {
        log.warn("Fallback triggered for processPayment, orderId={}, userId={}, amount={}, reason={}",
                orderId, userId, amount, t.toString());

        throw new PaymentServiceUnavailableException(
                "Payment Service is currently unavailable — could not process payment for order " + orderId, t
        );
    }
}