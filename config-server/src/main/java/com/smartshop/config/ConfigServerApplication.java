package com.smartshop.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server — centralized configuration for all SmartShop services.
 *
 * How it works:
 * 1. This server starts on port 8888 (Spring Cloud Config convention).
 * 2. It reads configuration files from the /configurations directory.
 * 3. When a microservice starts, it calls this server to fetch its config.
 *    Example: user-service calls GET http://localhost:8888/user-service/default
 * 4. The config server finds "user-service.yml" and returns its contents.
 *
 * @EnableConfigServer — Turns this Spring Boot app into a Config Server
 *                       that serves configuration over HTTP.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
