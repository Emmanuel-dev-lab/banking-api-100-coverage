package com.bank.application.service;

import com.bank.domain.exception.ClientNotFoundException;
import com.bank.domain.model.Client;
import com.bank.domain.model.Role;
import com.bank.domain.model.User;
import com.bank.support.Fakes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientServiceTest {

    private Fakes.InMemoryClientRepository clients;
    private Fakes.InMemoryUserRepository users;
    private Fakes.FakePasswordHasher hasher;
    private ClientService service;

    @BeforeEach
    void setUp() {
        clients = new Fakes.InMemoryClientRepository();
        users = new Fakes.InMemoryUserRepository();
        hasher = new Fakes.FakePasswordHasher();
        service = new ClientService(clients, users, new Fakes.SequentialIdGenerator(), hasher);
    }

    // CS1
    @Test
    void getClient_unknown_throws() {
        assertThatThrownBy(() -> service.getClient("nope"))
                .isInstanceOf(ClientNotFoundException.class);
    }

    // CS2
    @Test
    void getClient_existing_returns() {
        Client created = service.createClient("John", "Doe", "john", "pw");
        assertThat(service.getClient(created.id())).isSameAs(created);
    }

    // CS3
    @Test
    void createClient_valid_persistsClientAndUser() {
        Client created = service.createClient("John", "Doe", "john", "pw");
        assertThat(created.firstName()).isEqualTo("John");
        User user = users.findByUsername("john").orElseThrow();
        assertThat(user.role()).isEqualTo(Role.CLIENT);
        assertThat(user.clientId()).isEqualTo(created.id());
        assertThat(hasher.matches("pw", user.passwordHash())).isTrue();
    }
}
