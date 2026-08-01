package com.hackathon.energia.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI energiaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EnergiAI API")
                        .description("Análisis de eficiencia energética - Hackathon ONE G9")
                        .version("v0.1.0"));
    }
}