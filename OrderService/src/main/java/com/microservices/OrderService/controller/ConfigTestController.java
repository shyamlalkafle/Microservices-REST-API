package com.microservices.OrderService.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config-test")
public class ConfigTestController {

    @Value("${app.message:DEFAULT-MESSAGE}")
    private String message;

    @Value("${app.timeout:0}")
    private int timeout;

    @Value("${app.environment:unknown}")
    private String environment;

    @Value("${user-service.base-url:NOT-SET}")
    private String userServiceUrl;

    @Value("${payment-service.base-url:NOT-SET}")
    private String paymentServiceUrl;

    @Value("${order-service.rest-client.connect-timeout:0}")
    private int connectTimeout;

    @Value("${order-service.rest-client.read-timeout:0}")
    private int readTimeout;

    @GetMapping
    public Map<String, Object> getConfigValues() {
        Map<String, Object> config = new HashMap<>();
        config.put("message", message);
        config.put("timeout", timeout);
        config.put("environment", environment);
        config.put("userServiceUrl", userServiceUrl);
        config.put("paymentServiceUrl", paymentServiceUrl);
        config.put("connectTimeout", connectTimeout);
        config.put("readTimeout", readTimeout);
        config.put("source", "Config Server (centralized)");
        return config;
    }
}
