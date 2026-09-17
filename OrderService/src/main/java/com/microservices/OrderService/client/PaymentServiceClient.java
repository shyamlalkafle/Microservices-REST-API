package com.microservices.OrderService.client;

import com.microservices.OrderService.dto.PaymentRequest;
import com.microservices.OrderService.dto.PaymentResponse;
import com.microservices.OrderService.exception.PaymentServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Slf4j
@Component
public class PaymentServiceClient {

    private final RestClient restClient;

    public PaymentServiceClient(RestClient paymentServiceRestClient) {
        this.restClient = paymentServiceRestClient;
    }

    // NO RETRY for payment operations (avoid duplicate payments/double-charging)
    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    public boolean processPayment(Long orderId, Long userId, BigDecimal amount) {
        PaymentRequest request = new PaymentRequest(orderId, userId, amount);

        PaymentResponse response = restClient.post()
                .uri("/api/payments")
                .body(request)
                .retrieve()
                .body(PaymentResponse.class);

        return response != null && "SUCCESS".equals(response.getStatus());
    }

    // Fallback method - IMPORTANT: Signature must match original method params + Throwable
    private boolean processPaymentFallback(Long orderId, Long userId, BigDecimal amount, Throwable t) {
        log.warn("Fallback triggered for processPayment, orderId={}, userId={}, amount={}, reason={}",
                orderId, userId, amount, t.toString());

        throw new PaymentServiceUnavailableException(
                "Payment Service is currently unavailable — could not process payment for order " + orderId, t
        );
    }
}