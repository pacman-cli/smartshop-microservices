# SmartShop Microservices - Context & Instructions

This repository contains a production-ready e-commerce microservices platform built with Spring Boot 3, Spring Cloud, and Docker.

## Project Overview
SmartShop is a distributed system following the "Database-per-Service" and "API Gateway" patterns. It leverages service discovery, centralized configuration, and event-driven communication via Kafka.

### Tech Stack
- **Language/Runtime:** Java 17, Spring Boot 3.4.13
- **Microservices Framework:** Spring Cloud 2024.0.3 (Eureka, Config Server, Gateway, OpenFeign)
- **Security:** Spring Security + JWT (Gateway-level validation)
- **Persistence:** PostgreSQL 16 (Service-owned databases)
- **Messaging:** Apache Kafka (Asynchronous communication)
- **Resilience:** Resilience4j (Circuit Breakers, Retries)
- **Observability:** Prometheus, Grafana, Micrometer Tracing (Zipkin)
- **Build/Ops:** Maven (Multi-module), Docker, Docker Compose

## Architecture
- **Client Entry:** `api-gateway` (Port 8080) handles routing and JWT validation.
- **Infrastructure:** `discovery-server` (Eureka) and `config-server` (Centralized YAML).
- **Core Services:**
  - `user-service` (8081): Auth, JWT issuance, Profile management.
  - `product-service` (8082): Catalog and Stock management.
  - `order-service` (8083): Orchestrates orders using OpenFeign (Sync) and Kafka Outbox (Async).
  - `payment-service` (8084): Processes payments and publishes success events.
  - `notification-service` (8085): Consumes Kafka events to send emails.

## Building and Running

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### Commands
- **Build all modules:** `mvn clean package -DskipTests`
- **Run with Docker Compose:** `docker-compose up --build -d`
- **Watch logs:** `docker-compose logs -f`
- **Run single service (local):** `mvn spring-boot:run -pl <service-name>`

### Service Startup Order (Manual)
1. PostgreSQL & Kafka
2. `discovery-server`
3. `config-server`
4. `api-gateway`
5. Business Services (`user`, `product`, `order`, `payment`, `notification`)

## Development Conventions

### Coding Style
- **Lombok:** Used extensively to reduce boilerplate. Use `@Data`, `@Builder`, and `@RequiredArgsConstructor`.
- **DTOs:** Separate entities (persistence) from DTOs (API). Map using explicit methods or libraries.
- **Exceptions:** Global exception handling using `@RestControllerAdvice`.

### Security Pattern
- **JWT Validation:** Happens primarily at the `api-gateway` in `JwtAuthGatewayFilter`.
- **Header Propagation:** Gateway injects `X-User-Id`, `X-User-Email`, and `X-User-Role` into downstream requests.
- **Trust:** Downstream services trust these headers for authorization logic.

### Reliability
- **Transactional Outbox:** Implemented in `order-service` to ensure events are reliably published to Kafka.
- **Idempotency:** `product-service` handles stock reduction idempotently using keys provided by the order service.

## Testing Strategy
- **Unit/Integration Tests:** Run with `mvn test`. Focus on service layer and repository layer.
- **API Testing:** A Postman collection is available at `smartshop-postman-collection.json`.
- **Monitoring:** Actuator endpoints are exposed. Prometheus metrics are available at `/actuator/prometheus`.

## Important Links
- **API Gateway:** `http://localhost:8080`
- **Eureka Dashboard:** `http://localhost:8761`
- **MailHog (Emails):** `http://localhost:8025`
- **Grafana:** `http://localhost:3000` (admin/admin)
- **Prometheus:** `http://localhost:9090`
- **Zipkin:** `http://localhost:9411`
