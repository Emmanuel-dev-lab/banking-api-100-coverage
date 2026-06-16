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
}
