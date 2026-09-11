package com.microservices.OrderService.client;

import com.microservices.OrderService.dto.PaymentRequest;
import com.microservices.OrderService.dto.PaymentResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class PaymentServiceClient {

    private final RestClient restClient;

    public PaymentServiceClient(RestClient paymentServiceRestClient) {
        this.restClient = paymentServiceRestClient;
    }

    public boolean processPayment(Long orderId, Long userId, BigDecimal amount) {
        try {
            PaymentRequest request = new PaymentRequest(orderId, userId, amount);

            PaymentResponse response = restClient.post()
                    .uri("/api/payments")
                    .body(request)
                    .retrieve()
                    .body(PaymentResponse.class);

            return response != null && "SUCCESS".equals(response.getStatus());

        } catch (Exception e) {
            return false; // no retry here - POST is not idempotent, same reasoning as before
        }
    }
}