package com.smartshop.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration for Notification Service.
 * Access docs at: /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartShop Notification Service API")
                        .description("Kafka event consumer and email notification endpoints")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SmartShop Team")
                                .email("dev@smartshop.com")));
    }
}