package com.bank.domain.model;

import com.bank.domain.exception.AccountNotActiveException;
import com.bank.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class AccountTest {

    private CurrentAccount current(long balance, long overdraft) {
        return new CurrentAccount("a1", "c1", Money.of(balance), overdraft);
    }

    private SavingsAccount savings(long balance, double rate) {
        return new SavingsAccount("s1", "c1", Money.of(balance), rate);
    }

    // A1
    @Test
    void ensureActive_frozen_throws() {
        CurrentAccount a = current(100, 0);
        a.freeze();
        assertThatThrownBy(a::ensureActive).isInstanceOf(AccountNotActiveException.class);
    }

    // A2
    @Test
    void ensureActive_active_ok() {
        assertThatCode(current(100, 0)::ensureActive).doesNotThrowAnyException();
    }

    // A3
    @Test
    void close_nonZeroBalance_throws() {
        assertThatThrownBy(() -> current(100, 0).close())
                .isInstanceOf(IllegalStateException.class);
    }

    // A4
    @Test
    void close_zeroBalance_closed() {
        CurrentAccount a = current(0, 0);
        a.close();
        assertThat(a.status()).isEqualTo(AccountStatus.CLOSED);
    }

    // A5
    @Test
    void deposit_frozen_throws() {
        CurrentAccount a = current(100, 0);
        a.freeze();
        assertThatThrownBy(() -> a.deposit(Money.of(10)))
                .isInstanceOf(AccountNotActiveException.class);
    }

    // A6
    @Test
    void deposit_active_increasesBalance() {
        CurrentAccount a = current(100, 0);
        a.deposit(Money.of(50));
        assertThat(a.balance().amount()).isEqualTo(150);
    }

    // CA1
    @Test
    void currentWithdraw_withinOverdraft_ok() {
        CurrentAccount a = current(100, 50);
        a.withdraw(Money.of(120));
        assertThat(a.balance().amount()).isEqualTo(-20);
    }

    // CA2
    @Test
    void currentWithdraw_beyondOverdraft_throws() {
        CurrentAccount a = current(100, 50);
        assertThatThrownBy(() -> a.withdraw(Money.of(151)))
                .isInstanceOf(InsufficientFundsException.class);
    }

    // CA3 (frontiere exacte)
    @Test
    void currentWithdraw_exactlyAtOverdraftLimit_ok() {
        CurrentAccount a = current(100, 50);
        a.withdraw(Money.of(150));
        assertThat(a.balance().amount()).isEqualTo(-50);
    }

    // SA1
    @Test
    void savingsWithdraw_enough_ok() {
        SavingsAccount a = savings(100, 0.03);
        a.withdraw(Money.of(100));
        assertThat(a.balance().amount()).isZero();
    }

    // SA2
    @Test
    void savingsWithdraw_insufficient_throws() {
        SavingsAccount a = savings(100, 0.03);
        assertThatThrownBy(() -> a.withdraw(Money.of(101)))
                .isInstanceOf(InsufficientFundsException.class);
    }

    // SA3 : interets mensuels = floor(solde * tauxAnnuel/12)
    @Test
    void savingsApplyMonthlyInterest_positive_credits() {
        SavingsAccount a = savings(120000, 0.12); // 120000 * 0.01 = 1200
        Money credited = a.applyMonthlyInterest();
        assertThat(credited.amount()).isEqualTo(1200);
        assertThat(a.balance().amount()).isEqualTo(121200);
    }

    // SA4 : taux nul -> aucun credit, montant renvoye nul
    @Test
    void savingsApplyMonthlyInterest_zero_noChange() {
        SavingsAccount a = savings(1000, 0.0);
        Money credited = a.applyMonthlyInterest();
        assertThat(credited.amount()).isZero();
        assertThat(a.balance().amount()).isEqualTo(1000);
    }

    // SA5 : interet arrondi a l'inferieur, sous 1 unite -> aucun credit
    @Test
    void savingsApplyMonthlyInterest_belowOneUnit_noChange() {
        SavingsAccount a = savings(100, 0.03); // 100 * 0.0025 = 0.25 -> floor 0
        Money credited = a.applyMonthlyInterest();
        assertThat(credited.amount()).isZero();
        assertThat(a.balance().amount()).isEqualTo(100);
    }

    // reconstitution (persistance)
    @Test
    void restore_current_keepsStatusAndBalance() {
        CurrentAccount a = new CurrentAccount("a1", "c1", Money.of(500), AccountStatus.FROZEN, 100);
        assertThat(a.balance().amount()).isEqualTo(500);
        assertThat(a.status()).isEqualTo(AccountStatus.FROZEN);
        assertThat(a.overdraftLimit()).isEqualTo(100);
    }

    @Test
    void restore_savings_keepsStatusAndRate() {
        SavingsAccount a = new SavingsAccount("s1", "c1", Money.of(500), AccountStatus.CLOSED, 0.05);
        assertThat(a.balance().amount()).isEqualTo(500);
        assertThat(a.status()).isEqualTo(AccountStatus.CLOSED);
        assertThat(a.annualRate()).isEqualTo(0.05);
    }

    @Test
    void accessors() {
        CurrentAccount a = current(100, 50);
        assertThat(a.id()).isEqualTo("a1");
        assertThat(a.clientId()).isEqualTo("c1");
        assertThat(a.type()).isEqualTo(AccountType.CURRENT);
        assertThat(a.overdraftLimit()).isEqualTo(50);
        assertThat(a.status()).isEqualTo(AccountStatus.ACTIVE);
        SavingsAccount s = savings(100, 0.03);
        assertThat(s.type()).isEqualTo(AccountType.SAVINGS);
        assertThat(s.annualRate()).isEqualTo(0.03);
    }
}
