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

    // listing pagine
    @Test
    void listClients_returnsPage() {
        service.createClient("A", "A", "a", "pw");
        service.createClient("B", "B", "b", "pw");
        service.createClient("C", "C", "c", "pw");
        var page = service.listClients(0, 2);
        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
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

    // CS4 : mise a jour du nom d'un client existant
    @Test
    void updateClient_existing_changesName() {
        Client created = service.createClient("John", "Doe", "john", "pw");
        Client updated = service.updateClient(created.id(), "Johnny", "Doe-Smith");
        assertThat(updated.firstName()).isEqualTo("Johnny");
        assertThat(updated.lastName()).isEqualTo("Doe-Smith");
        assertThat(service.getClient(created.id()).firstName()).isEqualTo("Johnny");
    }

    // CS5 : mise a jour d'un client inconnu -> 404
    @Test
    void updateClient_unknown_throws() {
        assertThatThrownBy(() -> service.updateClient("nope", "A", "B"))
                .isInstanceOf(ClientNotFoundException.class);
    }
}
