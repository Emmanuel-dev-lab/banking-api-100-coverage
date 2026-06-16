package com.bank.application.service;

import com.bank.domain.exception.ForbiddenException;
import com.bank.domain.model.Role;
import com.bank.domain.port.TokenClaims;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationGuard {

    public void requireOwnerOrAdmin(TokenClaims claims, String ownerClientId) {
        if (claims.role() == Role.ADMIN) {
            return;
        }
        if (claims.clientId().equals(ownerClientId)) {
            return;
        }
        throw new ForbiddenException("access denied");
    }

    public void requireAdmin(TokenClaims claims) {
        if (claims.role() != Role.ADMIN) {
            throw new ForbiddenException("admin only");
        }
    }

    /** Renvoie le clientId du jeton, ou refuse si absent (ex. un ADMIN sur /me). */
    public String requireClientId(TokenClaims claims) {
        if (claims.clientId() == null) {
            throw new ForbiddenException("client account required");
        }
        return claims.clientId();
    }
}
