# SmartShop Microservices - Audit Before Coding

## 1. PROJECT STRUCTURE

```
smartshop-microservices/
├── discovery-server/        # Eureka registry (port 8761)
├── config-server/           # Spring Cloud Config (port 8888)
├── api-gateway/             # Gateway + JWT + rate limit (port 8080)
├── user-service/            # Auth + user CRUD (port 8081)
├── product-service/         # Catalog + stock (port 8082)
├── order-service/           # Orders + Feign + outbox (port 8083)
├── payment-service/         # Payment sim + Kafka (port 8084)
├── notification-service/    # Kafka consumer + email (port 8085)
├── smartshop-common-contracts/  # Shared DTOs, events, security
├── k8s/                     # K8s manifests
├── charts/smartshop/        # Helm chart
├── monitoring/              # Prometheus + Grafana
└── .github/workflows/       # CI pipeline
```

## 2. TECH STACK

- Java 17, Spring Boot 3.4.13, Spring Cloud 2024.0.3
- jjwt 0.12.5, Lombok, Logstash Logback, SpringDoc 2.8.6
- Flyway, Micrometer + Zipkin, Spring Boot Actuator
- PostgreSQL 16, Kafka, Redis, MailHog, Eureka, Spring Cloud Config

| Service | Key Deps |
|---------|----------|
| discovery-server | eureka-server |
| config-server | config-server, eureka-client |
| api-gateway | gateway, eureka-client, circuitbreaker-resilience4j, jjwt, redis-reactive |
| user-service | web, jpa, postgresql, security, jjwt, caffeine, flyway, springdoc |
| product-service | web, jpa, postgresql, redisson, flyway, springdoc |
| order-service | web, jpa, postgresql, openfeign, circuitbreaker-resilience4j, kafka, flyway |
| payment-service | web, jpa, postgresql, kafka, flyway |
| notification-service | web, kafka, mail |

## 3. SERVICE STATE

### discovery-server - DONE
Single Eureka server app. No business logic.

### config-server - DONE
Serves YAML configs for all services from `classpath:/configurations`.

### api-gateway - DONE
JWT validation filter (skips public paths), Redis rate limiting per IP, CORS, 6 routes.

### user-service - DONE (partial)
- Auth: register + login with BCrypt + JWT
- Caching: Caffeine (10min TTL, 1000 max)
- **Missing:** No update/delete endpoints. No admin enforcement.

### product-service - DONE
- CRUD + soft delete, SKU uniqueness, category enum
- Stock: single/batch reduce/restore with Redisson locks
- Search: keyword + category

### order-service - DONE
- Feign clients to product + user service with circuit breaker + bulkhead
- Outbox pattern (polls every 5s, retries up to 10)
- Kafka: publishes `order.created`, consumes `payment.completed`

### payment-service - DONE
- Simulated gateway (90% success rate)
- Kafka: publishes `payment.completed`
- No outbox pattern (fire-and-forget)

### notification-service - DONE
- Kafka consumer with DLT + retry (3 attempts, 2s backoff)
- Email via MailHog for order/payment confirmations

### smartshop-common-contracts - DONE
Shared DTOs (`PagedResponse`, `ErrorResponse`), events, `GatewayHeaderAuthFilter`, `BaseAuditEntity`.

## 4. SECURITY

**Auth Flow:**
1. Client -> gateway -> user-service (register/login, public)
2. user-service returns JWT (email, role, userId, 24h expiry)
3. Client sends `Authorization: Bearer <token>`
4. Gateway validates JWT, injects `X-User-Email`, `X-User-Role`, `X-User-Id` headers
5. Downstream services trust headers via `GatewayHeaderAuthFilter`

**Issues:**
- JWT secret hardcoded in multiple places
- RBAC roles (CUSTOMER/ADMIN) exist but NOT enforced on any endpoint
- No token revocation/blacklist
- No refresh token mechanism
- Any authenticated user can call any endpoint

## 5. TESTS

| Service | Tests | Type |
|---------|-------|------|
| discovery-server | Context load | SpringBootTest |
| config-server | Context load | SpringBootTest |
| api-gateway | Context load | SpringBootTest |
| user-service | UserServiceTest, AuthControllerTest | Unit + WebMvc |
| product-service | ProductServiceTest | Unit (Mockito) |
| order-service | OrderServiceTest | Unit (Mockito) |
| payment-service | PaymentServiceTest | Unit (Mockito) |
| notification-service | EmailServiceTest | Unit (Mockito) |

**Missing:**
- No integration tests (all mocked)
- No controller tests for product/order/payment
- No tests for JWT filters, outbox, Kafka
- OrderServiceTest and PaymentServiceTest are BROKEN (reference methods/fields that don't match current code)

## 6. CI/CD

GitHub Actions (`build.yml`):
- Push/PR to master/main
- PostgreSQL 16 + Redis 7 service containers
- JDK 17, Maven cache
- `mvn clean install -DskipTests` then `mvn test`

**Missing:** No Kafka in CI. No deployment step. No Docker build/push. No code quality tools.

## 7. MONITORING

- Prometheus scrapes all 8 services at `/actuator/prometheus`
- Grafana: empty provisioning dirs (no dashboards/datasources)
- Distributed tracing: Micrometer + Zipkin (100% sampling)
- Custom metrics: only order-service and product-service have them. user-service and payment-service metrics declared in README but NOT implemented.

## 8. CRITICAL ISSUES

1. **Tests broken** - OrderServiceTest/PaymentServiceTest won't compile
2. **RBAC not enforced** - roles exist but no `@PreAuthorize` anywhere
3. **JWT secret hardcoded** - security risk
4. **README metrics claims don't match code** - user/payment metrics missing
5. **No authorization on getOrderById** - any user can view any order

## 9. MODERATE ISSUES

6. user-service missing update/delete endpoints
7. Single PostgreSQL instance for all DBs (not production-ready)
8. Grafana dashboards empty
9. Helm chart bare-bones (no ConfigMaps, Secrets, probes, limits, Ingress)
10. K8s manifests have no health probes
11. Payment service no outbox pattern (event loss if Kafka down)
12. Config Server uses native (classpath) not Git-backed

## 10. MINOR ISSUES

13. Kafka auto-create topics enabled
14. No API versioning
15. OrderService throws RuntimeException instead of custom exceptions
16. No graceful shutdown config

## 11. INTER-SERVICE COMMUNICATION

**Synchronous (Feign + Eureka):**
- order-service -> product-service (get, batch get, reduce/restore stock)
- order-service -> user-service (get user)
- All with Resilience4j circuit breaker + bulkhead + fallbacks

**Asynchronous (Kafka):**
- `order.created` topic: order-service (outbox) -> notification-service
- `payment.completed` topic: payment-service -> order-service + notification-service
- Notification service has DLT + retry

## 12. PRIORITY FIX LIST

### P0 - Must Fix
- [ ] Fix broken tests (OrderServiceTest, PaymentServiceTest)
- [ ] Enforce RBAC with `@PreAuthorize` annotations
- [ ] Implement missing metrics (user-service, payment-service)
- [ ] Add authorization check on order access

### P1 - Should Fix
- [ ] Add user-service update/delete endpoints
- [ ] Add outbox pattern to payment-service
- [ ] Add Grafana dashboards
- [ ] Improve Helm chart (probes, limits, Ingress)
- [ ] Add K8s health probes

### P2 - Nice to Have
- [ ] Externalize JWT secret
- [ ] Add refresh token mechanism
- [ ] Add integration tests
- [ ] Add API versioning
- [ ] Custom exceptions for OrderService
- [ ] Graceful shutdown config
- [ ] Code quality tools in CI (Checkstyle, JaCoCo)
