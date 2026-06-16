package com.bank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI : metadonnees de l'API et schema de securite JWT.
 * La doc interactive est servie sur /swagger-ui.html ; le contrat JSON sur
 * /v3/api-docs. Classe de configuration (hors perimetre de couverture).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI bankingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Bancaire")
                        .version("1.0.0")
                        .description("""
                                API web bancaire (comptes, transactions, virements, prets) en XAF.

                                **Authentification** : appeler `POST /api/auth/login` pour obtenir un
                                jeton JWT, puis cliquer sur **Authorize** et saisir le jeton. Tous les
                                endpoints (hors login) exigent l'en-tete `Authorization: Bearer <token>`.

                                **Roles** : `CLIENT` (acces a ses propres comptes/prets) et `ADMIN`
                                (creation de clients, acces a tout).

                                **Montants** : entiers en XAF (Franc CFA, 0 decimale).""")
                        .contact(new Contact().name("ICT304 - Software Testing"))
                        .license(new License().name("Usage pedagogique")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Jeton JWT obtenu via POST /api/auth/login")));
    }
}
