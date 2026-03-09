package com.smartshop.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Discovery Server — the service registry for SmartShop.
 *
 * How it works:
 * 1. This server starts and listens on port 8761.
 * 2. Other microservices (user-service, product-service, etc.) register here on startup.
 * 3. When a service needs to call another service, it asks Eureka for the address.
 * 4. Eureka returns the location, enabling dynamic service discovery.
 *
 * @EnableEurekaServer — This single annotation turns a regular Spring Boot app
 *                       into a full Eureka Server with a built-in dashboard.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
