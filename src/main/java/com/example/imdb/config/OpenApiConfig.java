package com.example.imdb.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI imdbApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("IMDb Movie API")
                        .description("REST API for querying IMDb dataset")
                        .version("1.0.0"));
    }
}
