package com.bank.config;

import com.bank.application.service.AuthService;
import com.bank.web.RequestAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Verrou central d'authentification (default-deny). Tout endpoint sous /api/**
 * (hors /api/auth/**, exclu a l'enregistrement) exige un jeton valide AVANT
 * d'atteindre le controleur : un nouvel endpoint est protege par defaut, meme
 * si on oublie d'appeler authService dedans. L'autorisation fine (proprietaire
 * / ADMIN) reste a la charge des controleurs. Classe de config (hors couverture).
 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AuthenticationInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Leve UnauthorizedException (-> 401) si le jeton est absent ou invalide.
        authService.authenticate(RequestAuth.bearer(request.getHeader("Authorization")));
        return true;
    }
}
