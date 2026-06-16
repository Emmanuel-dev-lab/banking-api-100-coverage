package com.bank.application.service;

import com.bank.domain.exception.ForbiddenException;
import com.bank.domain.model.Role;
import com.bank.domain.port.TokenClaims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationGuardTest {

    private final AuthorizationGuard guard = new AuthorizationGuard();

    private TokenClaims admin() {
        return new TokenClaims("u-admin", null, Role.ADMIN);
    }

    private TokenClaims client(String clientId) {
        return new TokenClaims("u1", clientId, Role.CLIENT);
    }

    // AG1
    @Test
    void ownerOrAdmin_admin_ok() {
        assertThatCode(() -> guard.requireOwnerOrAdmin(admin(), "c1"))
                .doesNotThrowAnyException();
    }

    // AG2
    @Test
    void ownerOrAdmin_owner_ok() {
        assertThatCode(() -> guard.requireOwnerOrAdmin(client("c1"), "c1"))
                .doesNotThrowAnyException();
    }

    // AG3
    @Test
    void ownerOrAdmin_other_forbidden() {
        assertThatThrownBy(() -> guard.requireOwnerOrAdmin(client("c1"), "c2"))
                .isInstanceOf(ForbiddenException.class);
    }

    // AG4
    @Test
    void requireAdmin_client_forbidden() {
        assertThatThrownBy(() -> guard.requireAdmin(client("c1")))
                .isInstanceOf(ForbiddenException.class);
    }

    // AG5
    @Test
    void requireAdmin_admin_ok() {
        assertThatCode(() -> guard.requireAdmin(admin())).doesNotThrowAnyException();
    }
}
