# Eureka Service Discovery Setup Guide (Day 6)

## 🎯 Overview

This guide explains exactly what dependencies and configurations are needed for Eureka service discovery in the Microservices-REST-API project.

---

## 📦 Part 1: Discovery Server (Eureka Server)

### 1. Maven Dependency (pom.xml)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2025.1.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. Enable Eureka Server (DiscoveryServerApplication.java)

```java
package com.microservices.DiscoveryServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer  // ⭐ This annotation makes it Eureka Server
public class DiscoveryServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
```

### 3. Application Configuration (application.yaml)

```yaml
spring:
  application:
    name: DiscoveryServer

server:
  port: 8761  # Standard Eureka port

eureka:
  client:
    register-with-eureka: false  # Don't register itself as a client
    fetch-registry: false         # Don't fetch registry from other Eureka servers
```

### 4. Start and Verify

```bash
cd DiscoveryServer
mvn spring-boot:run
```

**Open Dashboard:** http://localhost:8761

You should see the Eureka dashboard with no instances registered yet.

---

## 📦 Part 2: Eureka Clients (UserService, OrderService, PaymentService, ApiGateway)

### 1. Maven Dependencies (pom.xml)

**Add to ALL client services:**

```xml
<dependencies>
    <!-- Eureka Client -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    
    <!-- For services that call other services (OrderService, PaymentService) -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2025.1.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. Application Configuration (application.yaml)

**Example for UserService:**

```yaml
spring:
  application:
    name: USER-SERVICE  # ⭐ This name appears in Eureka dashboard

server:
  port: 8081

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/  # ⭐ Eureka Server URL
```

**Example for OrderService:**

```yaml
spring:
  application:
    name: ORDER-SERVICE  # ⭐ Service name for Eureka

server:
  port: 8082

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/  # ⭐ Eureka Server URL
```

**Example for PaymentService:**

```yaml
spring:
  application:
    name: PAYMENT-SERVICE  # ⭐ Service name for Eureka

server:
  port: 8083

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/  # ⭐ Eureka Server URL
```

### 3. Service Registration (Automatic)

**That's it!** Services automatically:
- Register with Eureka on startup
- Send heartbeat every 30 seconds
- Appear in Eureka dashboard

**No additional code needed** - just the dependency and YAML config!

---

## 📦 Part 3: Using @LoadBalanced RestClient (OrderService, PaymentService)

These services need to **call other services** using Eureka service names.

### 1. Configuration (WebConfig.java)

**OrderService WebConfig:**

```java
package com.microservices.OrderService.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class WebConfig {

    // Default RestClient (not load-balanced)
    @Bean
    @Primary
    public RestClient.Builder defaultRestClientBuilder() {
        return buildBaseBuilder();
    }

    // ⭐ Load-balanced RestClient for Eureka service discovery
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return buildBaseBuilder();
    }

    private RestClient.Builder buildBaseBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder().requestFactory(factory);
    }

    // ⭐ RestClient that uses Eureka service name
    @Bean
    public RestClient userServiceRestClient(@LoadBalanced RestClient.Builder builder) {
        return builder.baseUrl("http://USER-SERVICE").build();  // Service name, not localhost
    }

    @Bean
    public RestClient paymentServiceRestClient(@LoadBalanced RestClient.Builder builder) {
        return builder.baseUrl("http://PAYMENT-SERVICE").build();  // Service name
    }
}
```

**PaymentService WebConfig:**

```java
package com.microservices.PaymentServices.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class WebConfig {

    @Bean
    @Primary
    public RestClient.Builder defaultRestClientBuilder() {
        return buildBaseBuilder();
    }

    // ⭐ Load-balanced RestClient for Eureka
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return buildBaseBuilder();
    }

    private RestClient.Builder buildBaseBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder().requestFactory(factory);
    }

    // ⭐ RestClient that uses Eureka service name
    @Bean
    public RestClient userServiceRestClient(@LoadBalanced RestClient.Builder builder) {
        return builder.baseUrl("http://USER-SERVICE").build();  // Service name
    }
}
```

### 2. Client Usage (No code changes needed!)

**PaymentServiceClient in OrderService:**

```java
@Component
public class PaymentServiceClient {
    private final RestClient restClient;  // Injected with Eureka-aware RestClient

    public PaymentServiceClient(RestClient paymentServiceRestClient) {
        this.restClient = paymentServiceRestClient;  // baseUrl = "http://PAYMENT-SERVICE"
    }

    public boolean processPayment(Long orderId, Long userId, BigDecimal amount) {
        try {
            PaymentRequest request = new PaymentRequest(orderId, userId, amount);
            
            // Eureka resolves PAYMENT-SERVICE to actual host:port (e.g., localhost:8083)
            PaymentResponse response = restClient.post()
                    .uri("/api/payments")
                    .body(request)
                    .retrieve()
                    .body(PaymentResponse.class);
            
            return response != null && "SUCCESS".equals(response.getStatus());
        } catch (Exception e) {
            return false;
        }
    }
}
```

**No code changes!** The RestClient injected via constructor already has:
- Base URL: `http://PAYMENT-SERVICE`
- `@LoadBalanced` annotation resolves service name via Eureka
- Automatically gets actual address: `http://localhost:8083`

---

## 📦 Part 4: API Gateway with Eureka

### 1. API Gateway Routes (application.yaml)

**Before (Day 5 - Hardcoded URLs):**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-route
          uri: http://localhost:8081  # ❌ Hardcoded
          predicates:
            - Path=/api/users/**
```

**After (Day 6 - Eureka Service Names):**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-route
          uri: lb://USER-SERVICE  # ✅ Load-balanced via Eureka
          predicates:
            - Path=/api/users/**
            
        - id: order-service-route
          uri: lb://ORDER-SERVICE  # ✅ Eureka resolves
          predicates:
            - Path=/api/orders/**
            
        - id: payment-service-route
          uri: lb://PAYMENT-SERVICE  # ✅ Eureka resolves
          predicates:
            - Path=/api/payments/**

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/  # ⭐ Gateway also registers with Eureka
```

**Key Point:** `lb://` prefix tells Spring Cloud Gateway to use load balancer (Eureka) to resolve service name.

---

## 📊 Service Name Mapping

| Service | `spring.application.name` | Eureka Registration | Used in RestClient | Used in Gateway |
|---------|--------------------------|---------------------|-------------------|----------------|
| UserService | `USER-SERVICE` | USER-SERVICE | `http://USER-SERVICE` | `lb://USER-SERVICE` |
| OrderService | `ORDER-SERVICE` | ORDER-SERVICE | `http://ORDER-SERVICE` | `lb://ORDER-SERVICE` |
| PaymentService | `PAYMENT-SERVICE` | PAYMENT-SERVICE | `http://PAYMENT-SERVICE` | `lb://PAYMENT-SERVICE` |
| API Gateway | `API-GATEWAY` | API-GATEWAY | N/A | N/A |

**Important Rules:**
1. `spring.application.name` MUST match the service name used in URLs
2. Service names are **case-sensitive**
3. Use UPPERCASE for clarity (convention)
4. Hyphens are allowed: `USER-SERVICE`, `user-service` both work

---

## 🔄 How It Works

### Step-by-Step Flow:

1. **Service Startup:**
   ```
   UserService starts → Reads application.yaml
   → Sees eureka.client.service-url.defaultZone
   → Connects to Eureka at http://localhost:8761/eureka/
   → Registers itself as "USER-SERVICE" with IP:PORT (localhost:8081)
   → Sends heartbeat every 30 seconds
   ```

2. **Service Discovery:**
   ```
   OrderService needs to call UserService
   → RestClient baseUrl = "http://USER-SERVICE"
   → @LoadBalanced intercepts the call
   → Asks Eureka: "What's the address of USER-SERVICE?"
   → Eureka responds: "localhost:8081"
   → Makes actual HTTP call to http://localhost:8081
   ```

3. **API Gateway Routing:**
   ```
   Client → GET http://localhost:8080/api/users/1
   → Gateway sees: uri: lb://USER-SERVICE
   → Asks Eureka: "Where is USER-SERVICE?"
   → Eureka: "localhost:8081"
   → Gateway forwards: GET http://localhost:8081/api/users/1
   → Returns response to client
   ```

---

## ✅ Verification Checklist

### 1. Eureka Dashboard Check

Open **http://localhost:8761**

You should see:
```
Application              AMIs        Availability Zones    Status
USER-SERVICE            n/a (1)     (1)                   UP (1) - localhost:user-service:8081
ORDER-SERVICE           n/a (1)     (1)                   UP (1) - localhost:order-service:8082
PAYMENT-SERVICE         n/a (1)     (1)                   UP (1) - localhost:payment-service:8083
API-GATEWAY             n/a (1)     (1)                   UP (1) - localhost:api-gateway:8080
```

### 2. Service Registration Test

```bash
# Create a user via UserService directly
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@example.com","balance":1000}'

# Response: {"id":1,"name":"Test",...}
```

### 3. Eureka Resolution Test

```bash
# Create an order via OrderService (calls USER-SERVICE via Eureka)
curl -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productName":"Laptop","quantity":1,"price":999.99}'

# Check logs - should see Eureka resolving USER-SERVICE
```

### 4. API Gateway Test

```bash
# Call UserService through API Gateway (Gateway uses Eureka)
curl http://localhost:8080/api/users/1

# Gateway resolves lb://USER-SERVICE via Eureka
```

---

## 🐛 Troubleshooting

### Service not appearing in Eureka Dashboard

**Check:**
1. Is Eureka Server running? (http://localhost:8761)
2. Is `eureka.client.service-url.defaultZone` correct in application.yaml?
3. Check service logs for connection errors
4. Wait 30-60 seconds for registration to complete

### "Service not found" or Connection refused errors

**Check:**
1. Is service name in `baseUrl` matching `spring.application.name`?
2. Is `@LoadBalanced` annotation present on RestClient.Builder?
3. Is `spring-cloud-starter-loadbalancer` dependency added?
4. Check Eureka dashboard - is target service UP?

### API Gateway returns 503 Service Unavailable

**Check:**
1. Is `lb://SERVICE-NAME` format correct in routes?
2. Is API Gateway registered with Eureka?
3. Is target service showing UP in Eureka?
4. Check Gateway logs for Eureka resolution errors

---

## 📝 Summary

### What You Need:

**Discovery Server:**
- Dependency: `spring-cloud-starter-netflix-eureka-server`
- Annotation: `@EnableEurekaServer`
- Config: `eureka.client.register-with-eureka: false`

**Client Services:**
- Dependency: `spring-cloud-starter-netflix-eureka-client`
- Config: `eureka.client.service-url.defaultZone: http://localhost:8761/eureka/`
- Application name: `spring.application.name: USER-SERVICE`

**Services Calling Other Services:**
- Additional dependency: `spring-cloud-starter-loadbalancer`
- Annotation: `@LoadBalanced` on RestClient.Builder
- Base URL: `http://SERVICE-NAME` (not localhost)

**API Gateway:**
- Routes: `uri: lb://SERVICE-NAME`
- Registered with Eureka like any client

That's it! Eureka handles the rest automatically. 🎉

---

**Last Updated:** September 2026  
**Project:** Microservices-REST-API  
**Day:** 6 - Service Discovery with Netflix Eureka
