# SmartShop Microservices Platform

Prod-ready e-commerce microservices platform. Built with Spring Boot 3, Spring Cloud, Docker.

## Architecture

```
Client → API Gateway (8080) → Service Discovery (Eureka)
                ↓
    ┌───────────┼───────────┬──────────────┐
    ↓           ↓           ↓              ↓
 User(8081) Product(8082) Order(8083) Payment(8084)
    │           │           │              │
 user_db    product_db   order_db      payment_db
                            │              │
                         Kafka ←───────────┘
                            ↓
                    Notification(8085) → Email (MailHog)
```

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.4.13 |
| Cloud | Spring Cloud 2024.0.3 |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Database | PostgreSQL 16 |
| Messaging | Apache Kafka |
| Service Discovery | Netflix Eureka |
| Configuration | Spring Cloud Config Server |
| Resilience | Resilience4j (Circuit Breaker) |
| Gateway | Spring Cloud Gateway |
| Build | Maven (multi-module) |
| Containers | Docker + Docker Compose |

## Services

| Service | Port | Description |
|---------|------|-------------|
| **discovery-server** | 8761 | Eureka service registry |
| **config-server** | 8888 | Centralized configuration |
| **api-gateway** | 8080 | Routing, JWT validation, CORS |
| **user-service** | 8081 | Registration, login, auth, CRUD |
| **product-service** | 8082 | Catalog, stock, search |
| **order-service** | 8083 | Order creation. Feign + Kafka |
| **payment-service** | 8084 | Payment processing + Kafka |
| **notification-service** | 8085 | Kafka consumer, email notifications |

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- (Optional) PostgreSQL 16, Kafka — or use Docker Compose

## Quick Start

### Option 1: Docker Compose (recommended)

```bash
# Build all service JARs
mvn clean package -DskipTests

# Start everything
docker-compose up --build -d

# Watch logs
docker-compose logs -f
```

Services start in order: postgres → zookeeper → kafka → discovery-server → config-server → business services.

### Option 2: Local Development

1. Start PostgreSQL, create databases:
```sql
CREATE DATABASE user_db;
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
```

2. Start Kafka + Zookeeper

3. Start services in order:
```bash
# 1. Discovery Server
mvn spring-boot:run -pl discovery-server

# 2. Config Server
mvn spring-boot:run -pl config-server

# 3. API Gateway
mvn spring-boot:run -pl api-gateway

# 4. Business services (any order)
mvn spring-boot:run -pl user-service
mvn spring-boot:run -pl product-service
mvn spring-boot:run -pl order-service
mvn spring-boot:run -pl payment-service
mvn spring-boot:run -pl notification-service
```

## API Endpoints

Requests go through API Gateway at `http://localhost:8080`.

### Auth (Public)
```
POST /api/auth/register    - Register new user
POST /api/auth/login       - Login, returns JWT token
```

### Users (Authenticated)
```
GET  /api/users            - List all users (ADMIN)
GET  /api/users/{id}       - Get user by ID
GET  /api/users/email/{e}  - Get user by email
PUT  /api/users/{id}       - Update user
DELETE /api/users/{id}     - Delete user
```

### Products (Public read, Authenticated write)
```
GET  /api/products              - List products (paginated)
GET  /api/products/{id}         - Get product by ID
GET  /api/products/sku/{sku}    - Get product by SKU
GET  /api/products/search?q=    - Search products
GET  /api/products/category/{c} - Filter by category
POST /api/products              - Create product
PUT  /api/products/{id}         - Update product
DELETE /api/products/{id}       - Delete product
```

### Orders (Authenticated)
```
POST /api/orders                - Create order
GET  /api/orders/{id}           - Get order by ID
GET  /api/orders/user/{userId}  - Get orders by user
GET  /api/orders                - List all orders
```

### Payments (Authenticated)
```
POST /api/payments              - Process payment
GET  /api/payments/{id}         - Get payment by ID
GET  /api/payments/order/{id}   - Get payment by order ID
GET  /api/payments              - List all payments
```

## Authentication

Use JWT Bearer tokens. Register and login to get token:

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@test.com","password":"password123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@test.com","password":"password123"}'

# Use token
curl http://localhost:8080/api/products \
  -H "Authorization: Bearer <token>"
```

## Key Patterns

- **Database per Service** — Service owns PostgreSQL database
- **API Gateway** — Entry point. JWT validation, route forwarding
- **Service Discovery** — Eureka for dynamic registration/lookup
- **Centralized Config** — Config Server serves YAML to services
- **Event-Driven** — Kafka topics `order.created`, `payment.completed` for async comms
- **Circuit Breaker** — Resilience4j on order-service. Prevent cascading failures
- **Caching** — Caffeine local cache on user-service. Fast lookups

## Swagger / OpenAPI Documentation

Each service exposes Swagger UI for API exploration:

| Service | Swagger URL |
|---------|-------------|
| User Service | http://localhost:8081/swagger-ui.html |
| Product Service | http://localhost:8082/swagger-ui.html |
| Order Service | http://localhost:8083/swagger-ui.html |
| Payment Service | http://localhost:8084/swagger-ui.html |
| Notification Service | http://localhost:8085/swagger-ui.html |

## Observability

### Prometheus Metrics
Access Prometheus: http://localhost:9090

Custom metrics exposed:
- `smartshop.users.registered` — User registrations
- `smartshop.users.logins` — Successful logins
- `smartshop.auth.failures` — Authentication failures
- `smartshop.orders.created` — Orders created
- `smartshop.orders.failed` — Failed order attempts
- `smartshop.payments.processed` — Payments processed
- `smartshop.payments.failed` — Failed payments
- `smartshop.payments.refunded` — Refunds issued

### Grafana Dashboards
Access Grafana: http://localhost:3000 (admin/admin)

### Zipkin Distributed Tracing
Access Zipkin: http://localhost:9411

## Docker Infrastructure

| Container | Image | Port |
|-----------|-------|------|
| PostgreSQL | postgres:16-alpine | 5432 |
| Zookeeper | confluentinc/cp-zookeeper:7.5.0 | 2181 |
| Kafka | confluentinc/cp-kafka:7.5.0 | 29092 |
| MailHog | mailhog/mailhog | 1025 (SMTP), 8025 (Web UI) |

MailHog UI: http://localhost:8025 — view notification-service emails.

## Build & Test

```bash
# Compile modules
mvn clean compile

# Run tests
mvn test

# Package JARs (skip tests)
mvn clean package -DskipTests
```

## Project Structure

```
smartshop-microservices/
├── pom.xml                    # Parent POM
├── docker-compose.yml         # Orchestration
├── init-databases.sql         # DB init
├── discovery-server/          # Eureka Server
├── config-server/             # Config Server
│   └── src/main/resources/configurations/  # Configs
├── api-gateway/               # Gateway + JWT
├── user-service/              # User + Auth
├── product-service/           # Catalog
├── order-service/             # Orders
├── payment-service/           # Payments
└── notification-service/      # Notifications
```

## Tutorial Progress

See [TUTORIAL_STEPS.md](TUTORIAL_STEPS.md). 37-step plan. Progress: 37/37. ✅ ALL COMPLETE!
