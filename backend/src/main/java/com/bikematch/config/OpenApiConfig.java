package com.bikematch.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI description served at {@code /v3/api-docs} and rendered by Swagger UI.
 * <p>
 * Like {@link SecurityConfig}, every operation requires a bearer JWT by default;
 * public controllers opt out with an empty {@code @SecurityRequirements}.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI bikeMatchOpenApi(@Value("${app.version}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("BikeMatch API")
                        .version(version)
                        .description("""
                                Rear-suspension kinematics for mountain bikes: analyse a side photo, \
                                save bikes privately, request publication and moderate the public catalog.

                                Validation and business errors are returned as Problem Details (RFC 9457). \
                                To try a protected operation, call `POST /api/auth/login`, copy the token \
                                and paste it in **Authorize**."""))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
