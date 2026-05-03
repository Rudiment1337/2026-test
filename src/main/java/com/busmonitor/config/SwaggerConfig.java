package com.busmonitor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Contact contact = new Contact()
            .name("Bus-2026")
            .email("exp@exp.com");
        Info info = new Info()
            .title("Bus Monitoring System API")
            .version("1.0")
            .description("API for monitoring bus sensors")
            .contact(contact);

        SecurityScheme securityScheme = new SecurityScheme()
            .name("BearerAuth")
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Enter JWT token: Bearer {token}");
        return new OpenAPI()
            .info(info)
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
            .schemaRequirement("BearerAuth", securityScheme);
    }
}
