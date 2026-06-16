package com.bank.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTest {

    // page < 0
    @Test
    void ctor_negativePage_throws() {
        assertThatThrownBy(() -> new PageRequest(-1, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // size < 1
    @Test
    void ctor_zeroSize_throws() {
        assertThatThrownBy(() -> new PageRequest(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // size > MAX
    @Test
    void ctor_tooLargeSize_throws() {
        assertThatThrownBy(() -> new PageRequest(0, PageRequest.MAX_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // valide + offset
    @Test
    void ctor_valid_offsetComputed() {
        PageRequest pr = new PageRequest(2, 20);
        assertThat(pr.page()).isEqualTo(2);
        assertThat(pr.size()).isEqualTo(20);
        assertThat(pr.offset()).isEqualTo(40);
    }
}
