# Config Server Properties Summary

## Properties Added to config-repo

All properties are now testable via ConfigTestController endpoints.

---

## Common Properties (all services)

### order-service.yaml
```yaml
app:
  message: "Hello from centralized config - order service"
  timeout: 3000

user-service:
  base-url: "http://user-service"

payment-service:
  base-url: "http://payment-service"

order-service:
  rest-client:
    connect-timeout: 5000
    read-timeout: 5000
```

### user-service.yaml
```yaml
app:
  message: "Hello from centralized config - user service"
  timeout: 3000

user-service:
  rest-client:
    connect-timeout: 5000
    read-timeout: 5000
```

### payment-service.yaml
```yaml
app:
  message: "Hello from centralized config - payment service"
  timeout: 3000

user-service:
  base-url: "http://user-service"

payment-service:
  rest-client:
    connect-timeout: 5000
    read-timeout: 5000
```

---

## Dev Profile Properties

### *-dev.yaml (all services)
```yaml
app:
  message: "Hello from centralized config - [service-name] - DEV profile"
  timeout: 3000
  environment: "development"
```

---

## Prod Profile Properties

### *-prod.yaml (all services)
```yaml
app:
  message: "Hello from centralized config - [service-name] - PROD profile"
  timeout: 5000
  environment: "production"
```

---

## Testing These Properties

### OrderService ConfigTestController
**Endpoint:** `GET http://localhost:8082/api/config-test`

**Tests:**
- `app.message` ✅
- `app.timeout` ✅
- `app.environment` ✅
- `user-service.base-url` ✅
- `payment-service.base-url` ✅
- `order-service.rest-client.connect-timeout` ✅
- `order-service.rest-client.read-timeout` ✅

**Expected Response (dev profile):**
```json
{
  "message": "Hello from centralized config - order service - DEV profile",
  "timeout": 3000,
  "environment": "development",
  "userServiceUrl": "http://user-service",
  "paymentServiceUrl": "http://payment-service",
  "connectTimeout": 5000,
  "readTimeout": 5000,
  "source": "Config Server (centralized)"
}
```

---

### UserService ConfigTestController
**Endpoint:** `GET http://localhost:8081/api/config-test`

**Tests:**
- `app.message` ✅
- `app.timeout` ✅
- `app.environment` ✅
- `user-service.rest-client.connect-timeout` ✅
- `user-service.rest-client.read-timeout` ✅

**Expected Response (dev profile):**
```json
{
  "message": "Hello from centralized config - user service - DEV profile",
  "timeout": 3000,
  "environment": "development",
  "connectTimeout": 5000,
  "readTimeout": 5000,
  "source": "Config Server (centralized)"
}
```

---

## Default Values (Fallback)

If Config Server is unavailable, ConfigTestController shows defaults:

```json
{
  "message": "DEFAULT-MESSAGE",
  "timeout": 0,
  "environment": "unknown",
  "userServiceUrl": "NOT-SET",
  "paymentServiceUrl": "NOT-SET",
  "connectTimeout": 0,
  "readTimeout": 0,
  "source": "Config Server (centralized)"
}
```

This proves the `optional:configserver` fallback is working!

---

## Profile Testing

**Change from dev to prod:**

1. Edit `application.yaml`:
   ```yaml
   spring:
     profiles:
       active: prod  # Change from 'dev' to 'prod'
   ```

2. Restart service

3. Call `/api/config-test` again:
   ```bash
   curl http://localhost:8082/api/config-test
   ```

4. Response now shows:
   ```json
   {
     "message": "Hello from centralized config - order service - PROD profile",
     "timeout": 5000,        // Changed from 3000
     "environment": "production",  // Changed from "development"
     ...
   }
   ```

---

**Last Updated:** September 2026  
**Related Files:**
- ConfigTestController (OrderService)
- ConfigTestController (UserService)
- config-repo/*.yaml files
