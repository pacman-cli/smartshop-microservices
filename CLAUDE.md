# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build all modules (skip tests for speed)
mvn clean package -DskipTests

# Compile only
mvn compile

# Run all tests
mvn test

# Test a single module
mvn test -pl user-service
mvn test -pl product-service

# Run a single test class
mvn test -pl user-service -Dtest=UserServiceTest

# Run a single test method
mvn test -pl user-service -Dtest=UserServiceTest#getUserByIdReturnsUser

# Full stack via Docker Compose
mvn clean package -DskipTests && docker-compose up --build -d

# View logs
docker-compose logs -f
docker-compose logs -f user-service
```

## Architecture

**Microservices platform** with 5 business services + 3 platform services. Java 17, Spring Boot 3.4.13, Spring Cloud 2024.0.3.

### Service Map

| Service | Port | Purpose |
|---------|------|---------|
| discovery-server | 8761 | Eureka service registry |
| config-server | 8888 | Centralized YAML config (classpath:/configurations/) |
| api-gateway | 8080 | JWT validation, rate limiting (Redis), route forwarding |
| user-service | 8081 | Auth + User CRUD, issues JWT |
| product-service | 8082 | Catalog + stock management, Redisson distributed locks |
| order-service | 8083 | Orders, Feign calls to product/user, outbox pattern → Kafka |
| payment-service | 8084 | Payment simulation, publishes to Kafka |
| notification-service | 8085 | Kafka consumer, sends email via MailHog |

### Key Patterns

- **Database-per-service**: each business service has its own PostgreSQL DB (user_db, product_db, order_db, payment_db)
- **Config Server**: all service configs in `config-server/src/main/resources/configurations/<service>.yml`. Local `application.properties` only sets `spring.application.name` and config import
- **Security flow**: user-service issues JWT → api-gateway validates and injects `X-User-Id`, `X-User-Email`, `X-User-Role` headers → downstream services trust headers via `GatewayHeaderAuthFilter` from `smartshop-common-contracts`. **Important**: `GatewayHeaderAuthFilter` is intentionally NOT a `@Component` — each service must register it as a bean in its own `SecurityConfig` class
- **Inter-service calls**: OpenFeign clients with Resilience4j circuit breaker + bulkhead (order-service → product-service, order-service → user-service)
- **Event-driven**: Kafka topics `order.created`, `payment.completed`. Order-service uses transactional outbox pattern (OutboxEvent + background poller)
- **Idempotency**: product-service tracks `idempotency_key` for stock reduce/restore operations
- **Migrations**: Flyway, versioned SQL in `src/main/resources/db/migration/` per service
- **Shared library**: `smartshop-common-contracts` module provides DTOs (`PagedResponse`, `ErrorResponse`), events (`OrderCreatedEvent`, `PaymentCompletedEvent`), `GatewayHeaderAuthFilter`, `BaseAuditEntity`

### Startup Order

postgres → zookeeper → kafka → redis → discovery-server → config-server → api-gateway → business services

### Common Dependencies (Parent POM)

All services inherit from the parent POM which provides: actuator, micrometer-registry-prometheus, springdoc-openapi, micrometer-tracing-bridge-brave, zipkin-reporter, logstash-logback-encoder, lombok. Child modules should NOT redeclare these.

### Package Namespace

All services: `com.smartshop.<service>` (e.g., `com.smartshop.user`, `com.smartshop.order`)
Common contracts: `com.smartshop.contracts`

## Observability

- **Prometheus** (port 9090): scrapes all services at `/actuator/prometheus`
- **Grafana** (port 3000): admin/admin, auto-provisioned
- **Zipkin** (port 9411): distributed tracing, all services sample at 100%
- **Swagger UI**: each service at `http://localhost:<port>/swagger-ui.html`
- **Custom business metrics**: via `MeterRegistryConfig` in order/payment/user services (counters for orders, payments, auth events)

## CI/CD

GitHub Actions (`.github/workflows/build.yml`): runs on push/PR to master. Uses PostgreSQL + Redis service containers. Runs `mvn clean install -DskipTests` then `mvn test`.

## Docker

All services use identical Dockerfile pattern: `eclipse-temurin:17-jre-alpine`, non-root user, healthcheck via wget to `/actuator/health`. `docker-compose.yml` defines 15 services on `smartshop-network` bridge.
