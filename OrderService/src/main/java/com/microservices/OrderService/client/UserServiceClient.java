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

/**
 * Client for User Service communication
 * Practice 11: Refactored to remove URL-building logic from methods
 * All URI patterns are defined as constants
 */
@Slf4j
@Component
public class UserServiceClient {

    // URI Pattern Constants - Practice 11: Centralized endpoint definitions
    private static final String USER_EXISTS_URI = "/api/users/{id}/exists";
    
    private final RestClient restClient;

    public UserServiceClient(RestClient userServiceRestClient) {
        this.restClient = userServiceRestClient;
    }

    /**
     * Check if user exists by ID
     * Practice 11: Clean method - no hardcoded URI patterns
     * 
     * @param userId User ID to check
     * @return true if user exists, false otherwise
     * @throws UserNotFoundException if user not found (404)
     * @throws UserServiceErrorException if service returns error
     * @throws UserServiceUnavailableException if service is unavailable (circuit breaker fallback)
     */
    @Retry(name = "userService")
    @CircuitBreaker(name = "userService", fallbackMethod = "userExistsFallback")
    public boolean userExists(Long userId) {
        log.debug("Checking if user exists: userId={}", userId);
        
        try {
            Boolean exists = restClient.get()
                    .uri(USER_EXISTS_URI, userId)
                    .retrieve()
                    .body(Boolean.class);
            
            log.debug("User existence check result: userId={}, exists={}", userId, exists);
            return Boolean.TRUE.equals(exists);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("User not found: userId={}", userId);
                throw new UserNotFoundException(
                        "User with id " + userId + " not found in User Service"
                );
            }
            log.error("Client error from User Service: userId={}, status={}", userId, e.getStatusCode());
            throw new UserServiceErrorException(
                    "Client error from User Service: " + e.getStatusCode()
            );

        } catch (HttpServerErrorException e) {
            log.error("Server error from User Service: userId={}, status={}", userId, e.getStatusCode());
            throw new UserServiceErrorException(
                    "User Service returned server error: " + e.getStatusCode()
            );
        }
    }

    /**
     * Fallback method for circuit breaker
     * Practice 11: Clean fallback signature
     * 
     * @param userId User ID that was being checked
     * @param t Throwable that caused the fallback
     * @return never returns, always throws exception
     */
    private boolean userExistsFallback(Long userId, Throwable t) {
        log.warn("Fallback triggered for userId={} due to: {}", userId, t.toString());
        throw new UserServiceUnavailableException(
                "User Service is currently unavailable — could not verify user " + userId, t
        );
    }
}