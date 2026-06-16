package com.bank.application.service;

import com.bank.domain.exception.UnauthorizedException;
import com.bank.domain.model.Role;
import com.bank.domain.model.User;
import com.bank.domain.port.TokenClaims;
import com.bank.support.Fakes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private Fakes.InMemoryUserRepository users;
    private Fakes.FakePasswordHasher hasher;
    private Fakes.FakeTokenService tokens;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = new Fakes.InMemoryUserRepository();
        hasher = new Fakes.FakePasswordHasher();
        tokens = new Fakes.FakeTokenService();
        service = new AuthService(users, hasher, tokens);
        users.save(new User("u1", "alice", hasher.hash("secret"), Role.CLIENT, "c1"));
    }

    // AS1
    @Test
    void login_unknownUser_unauthorized() {
        assertThatThrownBy(() -> service.login("bob", "secret"))
                .isInstanceOf(UnauthorizedException.class);
    }

    // AS2
    @Test
    void login_wrongPassword_unauthorized() {
        assertThatThrownBy(() -> service.login("alice", "wrong"))
                .isInstanceOf(UnauthorizedException.class);
    }

    // AS3
    @Test
    void login_valid_returnsToken() {
        String token = service.login("alice", "secret");
        assertThat(token).isEqualTo("tok-u1");
    }

    @Test
    void authenticate_returnsClaims() {
        String token = service.login("alice", "secret");
        TokenClaims claims = service.authenticate(token);
        assertThat(claims.userId()).isEqualTo("u1");
        assertThat(claims.clientId()).isEqualTo("c1");
        assertThat(claims.role()).isEqualTo(Role.CLIENT);
    }
}
