package com.smartshop.user;

import com.smartshop.contracts.config.CommonJpaAuditConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * User Service — handles registration, authentication, and user management.
 *
 * This is the first business microservice in SmartShop.
 * It connects to its own PostgreSQL database (user_db),
 * registers with Eureka, and fetches config from Config Server.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@Import(CommonJpaAuditConfig.class)
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
