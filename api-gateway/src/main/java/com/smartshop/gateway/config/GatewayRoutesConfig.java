package com.smartshop.gateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * Programmatic Route Configuration — currently unused.
 *
 * Routes are defined in application.properties. This class exists as a
 * placeholder showing how Java-based route definitions would work.
 * Enable programmatic routes by uncommenting the @Bean method and
 * removing the corresponding route definitions from application.properties.
 *
 * @see <a href="https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/configuration.html">Gateway Configuration</a>
 */
@Configuration
public class GatewayRoutesConfig {
    // Routes are configured via application.properties.
    // See GatewayRoutesConfig in implementation plan for Java-based alternative.
}
