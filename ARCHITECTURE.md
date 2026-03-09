# SmartShop Microservices Platform - System Architecture

## High-Level Architecture Diagram (ASCII)

```
                         ┌─────────────────────────────────┐
                         │          CLIENT APPS             │
                         │   (Web Browser / Mobile App)     │
                         └──────────────┬──────────────────┘
                                        │
                                        ▼
                         ┌─────────────────────────────────┐
                         │         API GATEWAY              │
                         │    (Spring Cloud Gateway)        │
                         │         Port: 8080               │
                         └──────────────┬──────────────────┘
                                        │
                    ┌───────────────────┼───────────────────────┐
                    │                   │                       │
                    ▼                   ▼                       ▼
        ┌───────────────┐   ┌───────────────┐       ┌───────────────┐
        │ USER SERVICE  │   │PRODUCT SERVICE│       │ ORDER SERVICE │
        │  Port: 8081   │   │  Port: 8082   │       │  Port: 8083   │
        │               │   │               │       │               │
        │ PostgreSQL    │   │ PostgreSQL    │       │ PostgreSQL    │
        │ (user_db)     │   │ (product_db)  │       │ (order_db)    │
        └───────────────┘   └───────────────┘       └───────┬───────┘
                                                            │
                                                    ┌───────▼───────┐
                                                    │PAYMENT SERVICE│
                                                    │  Port: 8084   │
                                                    │               │
                                                    │ PostgreSQL    │
                                                    │ (payment_db)  │
                                                    └───────┬───────┘
                                                            │
                                                      KAFKA EVENT
                                                   (payment.completed)
                                                            │
                                                    ┌───────▼────────────┐
                                                    │NOTIFICATION SERVICE│
                                                    │    Port: 8085      │
                                                    │                    │
                                                    │  (Listens to Kafka)│
                                                    └────────────────────┘

        ┌─────────────────────────────────────────────────────────────┐
        │                  INFRASTRUCTURE SERVICES                     │
        │                                                             │
        │  ┌─────────────────┐  ┌─────────────────┐                  │
        │  │ DISCOVERY SERVER│  │  CONFIG SERVER   │                  │
        │  │   (Eureka)      │  │ (Spring Cloud    │                  │
        │  │  Port: 8761     │  │  Config Server)  │                  │
        │  │                 │  │  Port: 8888      │                  │
        │  └─────────────────┘  └─────────────────┘                  │
        │                                                             │
        └─────────────────────────────────────────────────────────────┘
```

## Communication Patterns

### Synchronous (REST + OpenFeign)
- order-service → product-service  (check product availability)
- order-service → user-service     (validate user)
- order-service → payment-service  (initiate payment)

### Asynchronous (Kafka Events)
- payment-service → PUBLISHES → "payment.completed" event
- notification-service → LISTENS → "payment.completed" event
- order-service → PUBLISHES → "order.created" event
- notification-service → LISTENS → "order.created" event

## Service Registry (Eureka)
All microservices register themselves with Eureka Discovery Server.
The API Gateway uses Eureka to discover service locations dynamically.

## Config Server
All microservices pull their configuration from the centralized Config Server.
Config Server reads configuration from a Git repository or local files.

## Resilience (Resilience4j)
- Circuit Breaker: Prevents cascading failures
- Retry: Automatically retries failed requests
- Rate Limiter: Controls request throughput

## Security (Spring Security + JWT)
- user-service issues JWT tokens on login
- API Gateway validates JWT tokens
- Each service can verify tokens for protected endpoints

## Database-per-Service Pattern
| Service              | Database     | Port  |
|----------------------|-------------|-------|
| user-service         | user_db     | 5432  |
| product-service      | product_db  | 5433  |
| order-service        | order_db    | 5434  |
| payment-service      | payment_db  | 5435  |
| notification-service | (none)      | -     |

## Port Assignments
| Service              | Port |
|----------------------|------|
| discovery-server     | 8761 |
| config-server        | 8888 |
| api-gateway          | 8080 |
| user-service         | 8081 |
| product-service      | 8082 |
| order-service        | 8083 |
| payment-service      | 8084 |
| notification-service | 8085 |
| Kafka                | 9092 |
| Zookeeper            | 2181 |
| PostgreSQL           | 5432 |

## Technology Stack
- Java 17
- Spring Boot 3.x
- Spring Cloud 2023.x
- Spring Security + JWT
- Spring Data JPA + Hibernate
- PostgreSQL
- Apache Kafka
- Resilience4j
- Docker + Docker Compose
- Maven (Multi-module)
