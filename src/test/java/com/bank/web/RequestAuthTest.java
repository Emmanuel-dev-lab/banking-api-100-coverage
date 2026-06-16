package com.bank.web;

import com.bank.domain.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestAuthTest {

    // W1
    @Test
    void bearer_missingHeader_unauthorized() {
        assertThatThrownBy(() -> RequestAuth.bearer(null))
                .isInstanceOf(UnauthorizedException.class);
    }

    // W2
    @Test
    void bearer_badPrefix_unauthorized() {
        assertThatThrownBy(() -> RequestAuth.bearer("Basic abc"))
                .isInstanceOf(UnauthorizedException.class);
    }

    // W3
    @Test
    void bearer_valid_returnsToken() {
        assertThat(RequestAuth.bearer("Bearer xyz")).isEqualTo("xyz");
    }
}
