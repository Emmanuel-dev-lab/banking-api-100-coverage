package com.bank.domain.model;

import com.bank.domain.exception.InvalidAmountException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class MoneyTest {

    // M1
    @Test
    void of_negative_throws() {
        assertThatThrownBy(() -> Money.of(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // M2
    @Test
    void of_zeroOrPositive_ok() {
        assertThat(Money.of(0).amount()).isZero();
        assertThat(Money.of(10).amount()).isEqualTo(10);
    }

    // M3
    @Test
    void ofPositive_zero_throws() {
        assertThatThrownBy(() -> Money.ofPositive(0))
                .isInstanceOf(InvalidAmountException.class);
    }

    // M4
    @Test
    void ofPositive_positive_ok() {
        assertThat(Money.ofPositive(5).amount()).isEqualTo(5);
    }

    // M5
    @Test
    void isNegative_negative_true() {
        assertThat(Money.of(10).minus(Money.of(20)).isNegative()).isTrue();
    }

    // M6
    @Test
    void isNegative_positive_false() {
        assertThat(Money.of(10).isNegative()).isFalse();
    }

    // M7
    @Test
    void isGreaterThan_bothDirections() {
        assertThat(Money.of(10).isGreaterThan(Money.of(5))).isTrue();
        assertThat(Money.of(5).isGreaterThan(Money.of(10))).isFalse();
    }

    // M8
    @Test
    void isLessThan_bothDirections() {
        assertThat(Money.of(5).isLessThan(Money.of(10))).isTrue();
        assertThat(Money.of(10).isLessThan(Money.of(5))).isFalse();
    }

    @Test
    void plus_addsAmounts() {
        assertThat(Money.of(10).plus(Money.of(5)).amount()).isEqualTo(15);
    }

    @Test
    void zero_factory() {
        assertThat(Money.ZERO.amount()).isZero();
    }

    @Test
    void equality_byAmount() {
        assertThat(Money.of(10)).isEqualTo(Money.of(10));
        assertThatCode(() -> Money.of(10).hashCode()).doesNotThrowAnyException();
    }
}
