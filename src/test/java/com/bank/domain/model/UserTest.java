package com.bank.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private User admin() {
        return new User("u-admin", "admin", "hash", Role.ADMIN, null);
    }

    private User client(String clientId) {
        return new User("u1", "alice", "hash", Role.CLIENT, clientId);
    }

    // U1
    @Test
    void isAdmin_adminRole_true() {
        assertThat(admin().isAdmin()).isTrue();
    }

    // U2
    @Test
    void isAdmin_clientRole_false() {
        assertThat(client("c1").isAdmin()).isFalse();
    }

    // U3
    @Test
    void owns_adminNullClientId_false() {
        assertThat(admin().owns("c1")).isFalse();
    }

    // U4
    @Test
    void owns_matching_true() {
        assertThat(client("c1").owns("c1")).isTrue();
    }

    // U5
    @Test
    void owns_other_false() {
        assertThat(client("c1").owns("c2")).isFalse();
    }

    @Test
    void accessors() {
        User u = client("c1");
        assertThat(u.id()).isEqualTo("u1");
        assertThat(u.username()).isEqualTo("alice");
        assertThat(u.passwordHash()).isEqualTo("hash");
        assertThat(u.role()).isEqualTo(Role.CLIENT);
        assertThat(u.clientId()).isEqualTo("c1");
    }
}
