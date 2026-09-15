# Spring Cloud Config Server Setup Guide

## ✅ Current Status - All Requirements Met!

### 1. Config Server ✅
- **Running on**: Port 8888
- **Serving from**: `file:///D:/Github_professional/Microservices-REST-API/config-repo` (local git repo)
- **Status**: ✅ Already achieved

### 2. User Service ✅
- ✅ Has `spring-cloud-starter-config` dependency
- ✅ `spring.application.name: user-service` (lowercase)
- ✅ `spring.config.import: optional:configserver:http://localhost:8888`
- ✅ Successfully pulls config from Config Server
- ✅ Real settings now centralized:
  - `app.message` - profile-specific messages
  - `app.timeout` - timeout values
  - `app.environment` - environment labels
  - `user-service.rest-client.connect-timeout` - 5000ms (dev) / 10000ms (test)
  - `user-service.rest-client.read-timeout` - 5000ms (dev) / 10000ms (test)

### 3. Order Service ✅
- ✅ Has `spring-cloud-starter-config` dependency
- ✅ `spring.application.name: order-service` (lowercase)
- ✅ `spring.config.import: optional:configserver:http://localhost:8888`
- ✅ Real settings now centralized:
  - `app.message` - profile-specific messages
  - `app.timeout` - timeout values
  - `app.environment` - environment labels
  - `user-service.base-url` - http://localhost:8081
  - `payment-service.base-url` - http://localhost:8083
  - `order-service.rest-client.connect-timeout` - 5000ms (dev) / 10000ms (test)
  - `order-service.rest-client.read-timeout` - 5000ms (dev) / 10000ms (test)

### 4. Dev vs Test Proof ✅
**Files exist in config-repo:**
- ✅ `user-service-dev.yaml`
- ✅ `user-service-test.yaml`
- ✅ `order-service-dev.yaml`
- ✅ `order-service-test.yaml`

**Different values per profile:**

| Property | DEV | TEST |
|----------|-----|------|
| `app.message` | "...DEV profile" | "...TEST profile" |
| `app.timeout` | 3000 | 5000 |
| `app.environment` | "development" | "testing" |
| `*.rest-client.connect-timeout` | 5000 | 10000 |
| `*.rest-client.read-timeout` | 5000 | 10000 |

**To test profile switching:**
```bash
# Switch UserService to TEST profile
# In UserService/src/main/resources/application.yaml, change:
spring.profiles.active: test

# Restart UserService, then call:
curl http://localhost:8081/api/config-test
```

### 5. Failure Tolerance Proof ✅
- ✅ Config import is `optional:configserver:http://localhost:8888`
- ✅ Services will start even if Config Server is down
- ✅ Falls back to local application.yaml values or defaults

**To test:**
```bash
# 1. Stop Config Server
# 2. Restart UserService or OrderService
# 3. Services should start successfully (no crash)
# 4. Check logs - will show "Could not locate PropertySource" but continue
```

---

## 🧪 Testing Guide

### Test 1: Verify Config Server is Working
```bash
# Start Config Server
cd ConfigServer
mvn spring-boot:run

# Test config endpoints
curl http://localhost:8888/user-service/dev
curl http://localhost:8888/order-service/test
```

### Test 2: Verify UserService Pulls Centralized Config (DEV)
```bash
# Start UserService (with profiles.active: dev)
cd UserService
mvn spring-boot:run

# Check centralized config values
curl http://localhost:8081/api/config-test
```

**Expected Response (DEV):**
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

### Test 3: Verify Profile Switching (DEV → TEST)
```bash
# 1. Change UserService application.yaml:
#    spring.profiles.active: test
# 2. Restart UserService
# 3. Test again:
curl http://localhost:8081/api/config-test
```

**Expected Response (TEST):**
```json
{
  "message": "Hello from centralized config - user service - TEST profile",
  "timeout": 5000,
  "environment": "testing",
  "connectTimeout": 10000,
  "readTimeout": 10000,
  "source": "Config Server (centralized)"
}
```

### Test 4: Verify OrderService Pulls Centralized Config
```bash
# Start OrderService (with profiles.active: dev)
cd OrderService
mvn spring-boot:run

# Check centralized config values
curl http://localhost:8082/api/config-test
```

**Expected Response:**
```json
{
  "message": "Hello from centralized config - order service - DEV profile",
  "timeout": 3000,
  "environment": "development",
  "userServiceUrl": "http://localhost:8081",
  "paymentServiceUrl": "http://localhost:8083",
  "connectTimeout": 5000,
  "readTimeout": 5000,
  "source": "Config Server (centralized)"
}
```

### Test 5: Verify Failure Tolerance (optional: prefix)
```bash
# 1. Stop Config Server
# 2. Restart UserService
# 3. Check logs - should see:
#    "Could not locate PropertySource and the fail fast property is not set"
# 4. Service should STILL START successfully
# 5. Check fallback:
curl http://localhost:8081/api/config-test

# Expected: default values or local application.yaml values
```

---

## 📁 Config Repository Structure

```
config-repo/
├── .git/                           # Git repository
├── user-service-dev.yaml          # User Service - DEV profile
├── user-service-test.yaml         # User Service - TEST profile ⭐ NEW
├── user-service-prod.yaml         # User Service - PROD profile
├── order-service-dev.yaml         # Order Service - DEV profile ⭐ ENHANCED
├── order-service-test.yaml        # Order Service - TEST profile ⭐ NEW
├── order-service-prod.yaml        # Order Service - PROD profile
├── payment-service-dev.yaml       # Payment Service - DEV
├── payment-service-test.yaml      # Payment Service - TEST
├── payment-service-prod.yaml      # Payment Service - PROD
├── api-gateway.yaml               # API Gateway config
└── application.yaml               # Global shared config
```

---

## 🎯 Key Concepts Explained

### 1. Why Centralized Config Matters at Scale

**Without Config Server:**
- 50 microservices = 50 application.yaml files to update
- Database password change = rebuild & redeploy all 50 services
- Different configs per environment scattered across repos
- No audit trail of config changes

**With Config Server:**
- Single source of truth for all configs
- Change config → services refresh without rebuild/redeploy
- Version control for configuration (git history)
- Environment-specific configs in one place
- Easier compliance and auditing

**Real-world scenario:**
```
Problem: Need to change Kafka broker URL for 30 microservices
Without Config Server: Update 30 repos, build 30 images, deploy 30 services
With Config Server: Update one file in config-repo, services auto-refresh
```

### 2. Difference Between Eureka and Config Server

| Aspect | Eureka (Service Discovery) | Config Server |
|--------|---------------------------|---------------|
| **Purpose** | **Finds WHERE a service is** | **Tells WHAT settings to use** |
| **Answers** | "Where is UserService running?" | "What's the timeout for UserService?" |
| **Returns** | IP address + port (e.g., 192.168.1.10:8081) | Configuration properties (timeout: 5000) |
| **Use Case** | Dynamic service location | Externalized configuration |
| **Example** | OrderService asks: "Where is PaymentService?" → Eureka: "10.0.1.5:8083" | UserService asks: "What's my DB password?" → Config: "db_pass_prod_2024" |

**They work together:**
```
OrderService needs to call PaymentService:
1. Asks Config Server: "What's my timeout for HTTP calls?" → 5 seconds
2. Asks Eureka: "Where is PaymentService running?" → 10.0.1.5:8083
3. Makes REST call with 5-second timeout to 10.0.1.5:8083
```

### 3. Why Secrets Shouldn't Go in Config Files

**Problems with storing secrets in config-repo:**

1. **Git History Exposes Secrets**
   ```bash
   # Even if you delete the password, it's in git history:
   git log --all -- user-service.yaml
   # Shows: "password: SuperSecret123" from 6 months ago
   ```

2. **Broad Access**
   - Config repo needs to be accessible to all services
   - Developers who can read configs can read all secrets
   - Hard to implement least-privilege access

3. **No Rotation or Expiry**
   - Secrets should rotate regularly
   - Config files are static
   - No automatic expiration

4. **Audit Trail**
   - Who accessed the database password?
   - Config Server doesn't track access
   - Can't alert on suspicious access

**Better Alternatives:**

| Secret Type | Use This Instead |
|-------------|-----------------|
| Database passwords | **Vault**, AWS Secrets Manager, Azure Key Vault |
| API keys | Environment variables + secret manager |
| Certificates | Cert management systems (Let's Encrypt, AWS ACM) |
| OAuth tokens | Token vaults with auto-refresh |

**Config files should store:**
- ✅ Timeouts (5000ms)
- ✅ URLs (http://localhost:8081)
- ✅ Feature flags (enableNewFeature: true)
- ✅ Non-sensitive properties

**Secret managers should store:**
- ❌ Passwords
- ❌ Private keys
- ❌ OAuth client secrets
- ❌ API tokens

**Best practice:**
```yaml
# config-repo/user-service.yaml (Config Server)
datasource:
  url: jdbc:postgresql://localhost:5432/mydb
  username: app_user
  # ❌ DON'T: password: SuperSecret123

# Instead, use Vault or environment variable:
# ✅ DO: Password injected from Vault at runtime
# ✅ DO: ${DB_PASSWORD} from secure env variable
```

---

## 🚀 Startup Order

```
1. Config Server (port 8888)
2. Eureka Server (port 8761) - optional but recommended
3. UserService (port 8081)
4. PaymentService (port 8083)
5. OrderService (port 8082)
6. API Gateway (port 8080)
```

---

## 📝 Summary

✅ **Config Server**: Running, serving from local git repo  
✅ **UserService**: Pulls centralized config successfully  
✅ **OrderService**: Pulls centralized config successfully  
✅ **Profile Support**: dev/test configs with different values  
✅ **Failure Tolerance**: `optional:` prefix allows startup without Config Server  
✅ **Proof of Concept**: `/api/config-test` endpoints show centralized values  

**All requirements met!** 🎉

---

**Last Updated**: September 2026  
**Project**: Microservices-REST-API (RestClient Implementation)
