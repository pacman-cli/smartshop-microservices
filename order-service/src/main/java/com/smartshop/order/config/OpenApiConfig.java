package com.smartshop.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration for Order Service.
 * Access docs at: /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartShop Order Service API")
                        .description("Order creation and order history endpoints")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SmartShop Team")
                                .email("dev@smartshop.com")));
    }
}