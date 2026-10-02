package co.edu.eci.blueprints.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI blueprintsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("BluePrints RT API")
                .version("v1")
                .description("API REST de planos + colaboracion en tiempo real con STOMP (/ws-blueprints). ARSW Lab P4."));
    }
}
