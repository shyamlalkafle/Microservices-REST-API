package com.microservices.PaymentServices.client;

import com.microservices.PaymentServices.exception.UserServiceUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(RestClient userServiceRestClient) {
        this.restClient = userServiceRestClient;
    }

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
}