package com.smartshop.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration for Payment Service.
 * Access docs at: /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartShop Payment Service API")
                        .description("Payment processing and transaction endpoints")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SmartShop Team")
                                .email("dev@smartshop.com")));
    }
}