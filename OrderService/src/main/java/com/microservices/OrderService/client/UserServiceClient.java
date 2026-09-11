package com.microservices.OrderService.client;

import com.microservices.OrderService.exception.UserNotFoundException;
import com.microservices.OrderService.exception.UserServiceErrorException;
import com.microservices.OrderService.exception.UserServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(RestClient userServiceRestClient) {
        this.restClient = userServiceRestClient;
    }

    public boolean userExists(Long userId) {
        int maxRetries = 2;
        int retryCount = 0;
        long retryDelayMillis = 500;

        while (retryCount <= maxRetries) {
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

            } catch (ResourceAccessException e) {
                retryCount++;
                if (retryCount > maxRetries) {
                    throw new UserServiceUnavailableException(
                            "User Service still unreachable after retries", e
                    );
                }
                try {
                    Thread.sleep(retryDelayMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new UserServiceUnavailableException(
                            "Retry interrupted while connecting to User Service", ie
                    );
                }
            }
        }

        throw new UserServiceUnavailableException("Unable to connect to User Service");
    }
}