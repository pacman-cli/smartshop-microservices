# SmartShop Microservices Platform

A production-ready e-commerce microservices platform built with Spring Boot 3, Spring Cloud, and Docker.

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
| **api-gateway** | 8080 | Request routing, JWT validation, CORS |
| **user-service** | 8081 | Registration, login, JWT auth, user CRUD |
| **product-service** | 8082 | Product catalog, stock management, search |
| **order-service** | 8083 | Order creation with Feign calls + Kafka events |
| **payment-service** | 8084 | Payment processing + Kafka events |
| **notification-service** | 8085 | Kafka consumer, sends email notifications |

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

Services will start in dependency order: postgres → zookeeper → kafka → discovery-server → config-server → all business services.

### Option 2: Local Development

1. Start PostgreSQL and create databases:
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

All requests go through the API Gateway at `http://localhost:8080`.

### Auth (Public)
```
POST /api/auth/register    - Register a new user
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

Use JWT Bearer tokens. Register and login to get a token:

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@test.com","password":"password123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@test.com","password":"password123"}'

# Use the token
curl http://localhost:8080/api/products \
  -H "Authorization: Bearer <token>"
```

## Key Patterns

- **Database per Service** — Each service owns its own PostgreSQL database
- **API Gateway** — Single entry point with JWT validation and route forwarding
- **Service Discovery** — Eureka for dynamic service registration/lookup
- **Centralized Config** — Config Server serves YAML configs to all services
- **Event-Driven** — Kafka topics `order.created` and `payment.completed` for async communication
- **Circuit Breaker** — Resilience4j on order-service Feign clients to prevent cascading failures
- **Caching** — Caffeine local cache on user-service for read-heavy lookups

## Docker Infrastructure

| Container | Image | Port |
|-----------|-------|------|
| PostgreSQL | postgres:16-alpine | 5432 |
| Zookeeper | confluentinc/cp-zookeeper:7.5.0 | 2181 |
| Kafka | confluentinc/cp-kafka:7.5.0 | 29092 |
| MailHog | mailhog/mailhog | 1025 (SMTP), 8025 (Web UI) |

MailHog web UI: http://localhost:8025 — view all emails sent by notification-service.

## Build & Test

```bash
# Compile all modules
mvn clean compile

# Run all tests
mvn test

# Package JARs (skip tests)
mvn clean package -DskipTests
```

## Project Structure

```
smartshop-microservices/
├── pom.xml                    # Parent POM
├── docker-compose.yml         # Full stack orchestration
├── init-databases.sql         # PostgreSQL init script
├── discovery-server/          # Eureka Server
├── config-server/             # Spring Cloud Config
│   └── src/main/resources/configurations/  # Service configs
├── api-gateway/               # Spring Cloud Gateway + JWT filter
├── user-service/              # User management + Auth
├── product-service/           # Product catalog
├── order-service/             # Order processing
├── payment-service/           # Payment processing
└── notification-service/      # Email notifications via Kafka
```

## Tutorial Progress

See [TUTORIAL_STEPS.md](TUTORIAL_STEPS.md) for the 37-step implementation plan and current progress (33/37 complete).
