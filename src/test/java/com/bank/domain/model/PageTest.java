package com.bank.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageTest {

    @Test
    void totalPages_roundsUp() {
        Page<String> page = new Page<>(List.of("a", "b"), 50, 0, 20);
        assertThat(page.totalPages()).isEqualTo(3); // ceil(50/20)
        assertThat(page.content()).containsExactly("a", "b");
        assertThat(page.totalElements()).isEqualTo(50);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
    }

    @Test
    void totalPages_exactMultiple() {
        Page<String> page = new Page<>(List.of(), 40, 0, 20);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void totalPages_empty() {
        Page<String> page = new Page<>(List.of(), 0, 0, 20);
        assertThat(page.totalPages()).isZero();
    }
}
