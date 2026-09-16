package com.microservices.OrderService.client;

import com.microservices.OrderService.exception.UserNotFoundException;
import com.microservices.OrderService.exception.UserServiceErrorException;
import com.microservices.OrderService.exception.UserServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(RestClient userServiceRestClient) {
        this.restClient = userServiceRestClient;
    }

    @Retry(name = "userService")
    @CircuitBreaker(name = "userService", fallbackMethod = "userExistsFallback")
    public boolean userExists(Long userId) {
        try {
            Boolean exists = restClient.get()
                    .uri("/api/users/{id}/exists", userId)
                    .retrieve()
                    .body(Boolean.class);
            return Boolean.TRUE.equals(exists);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new UserNotFoundException(
                        "User with id " + userId + " not found in User Service"
                );
            }
            throw new UserServiceErrorException(
                    "Client error from User Service: " + e.getStatusCode()
            );

        } catch (HttpServerErrorException e) {
            throw new UserServiceErrorException(
                    "User Service returned server error: " + e.getStatusCode()
            );
        }
    }

    // Fallback signature must match: same params + Throwable, same return type
    private boolean userExistsFallback(Long userId, Throwable t) {
        log.warn("Fallback triggered for userId={} due to: {}", userId, t.toString());
        throw new UserServiceUnavailableException(
                "User Service is currently unavailable — could not verify user " + userId, t
        );
    }
}