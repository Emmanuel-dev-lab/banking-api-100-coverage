package com.bank.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientTest {

    // C1
    @Test
    void ctor_emptyFirstName_throws() {
        assertThatThrownBy(() -> new Client("c1", " ", "Doe"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // C2
    @Test
    void ctor_emptyLastName_throws() {
        assertThatThrownBy(() -> new Client("c1", "John", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // C1b (sous-condition null vraie)
    @Test
    void ctor_nullFirstName_throws() {
        assertThatThrownBy(() -> new Client("c1", null, "Doe"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // C2b (sous-condition null vraie)
    @Test
    void ctor_nullLastName_throws() {
        assertThatThrownBy(() -> new Client("c1", "John", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // C3
    @Test
    void ctor_valid_ok() {
        Client c = new Client("c1", "John", "Doe");
        assertThat(c.id()).isEqualTo("c1");
        assertThat(c.firstName()).isEqualTo("John");
        assertThat(c.lastName()).isEqualTo("Doe");
    }
}
