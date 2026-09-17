package com.microservices.PaymentServices.client;

import com.microservices.PaymentServices.exception.UserServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Slf4j
@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(RestClient userServiceRestClient) {
        this.restClient = userServiceRestClient;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "deductBalanceFallback")
    public boolean deductBalance(Long userId, BigDecimal amount) {
        try {
            Boolean success = restClient.patch()
                    .uri("/api/users/{id}/deduct?amount={amount}", userId, amount)
                    .retrieve()
                    .body(Boolean.class);

            return Boolean.TRUE.equals(success);

        } catch (ResourceAccessException e) {
            throw new UserServiceUnavailableException(
                    "Unable to connect to User Service. Service may be down or unreachable.", e
            );
        } catch (Exception e) {
            return false;
        }
    }

    private boolean deductBalanceFallback(Long userId, BigDecimal amount, Throwable t) {
        log.warn("Fallback triggered for deductBalance, userId={}, amount={}, reason={}",
                userId, amount, t.toString());
        
        throw new UserServiceUnavailableException(
                "User Service is currently unavailable — could not deduct balance for user " + userId, t
        );
    }
}