package com.skillnez.cloudstorage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {

        String securityScheme = "SESSION";

        return new OpenAPI().info(
                new Info()
                        .title("Cloud Storage API")
                        .version("1.0.0")
                        .description("Cloud Storage API Documentation"))
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(securityScheme))
                .components(
                        new io.swagger.v3.oas.models.Components()
                                .addSecuritySchemes(securityScheme,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.APIKEY)
                                                .in(SecurityScheme.In.COOKIE)
                                                .name("SESSION")));

    }

}
