package com.smartshop.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway — the single entry point for all SmartShop client requests.
 *
 * How it works:
 * 1. Client sends a request to http://localhost:8080/api/products
 * 2. Gateway checks its route configuration.
 * 3. It finds that /api/products/** routes to "PRODUCT-SERVICE".
 * 4. It asks Eureka: "Where is PRODUCT-SERVICE?"
 * 5. Eureka replies: "192.168.1.5:8082"
 * 6. Gateway forwards the request to http://192.168.1.5:8082/api/products
 * 7. Gateway returns the response to the client.
 *
 * @EnableDiscoveryClient — Registers this gateway with Eureka so it can
 *                          discover other services by name.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
