package com.bank.web.controller;

import com.bank.application.service.AuthService;
import com.bank.web.dto.LoginRequest;
import com.bank.web.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Obtention d'un jeton JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements // endpoint public : pas de jeton requis
    @Operation(summary = "Se connecter",
            description = "Verifie les identifiants et renvoie un jeton JWT a utiliser "
                    + "dans l'en-tete Authorization des autres appels.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion reussie, jeton renvoye"),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.username(), request.password());
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
