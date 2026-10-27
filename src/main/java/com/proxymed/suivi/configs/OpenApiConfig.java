package com.proxymed.suivi.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bouton "Authorize" de Swagger : coller l'accessToken recu de /api/auth/connexion du service
 * d'authentification (sans le prefixe "Bearer "). Le badge est envoye sur toutes les operations.
 */
@Configuration
public class OpenApiConfig {

    public static final String SCHEMA_BADGE = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PROXYMED Suivi API")
                        .description("Fiche 2 : visite de suivi mensuel a domicile. Toutes les adresses "
                                + "demandent le badge (JWT) delivre par le service d'authentification")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(SCHEMA_BADGE,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEMA_BADGE));
    }
}
