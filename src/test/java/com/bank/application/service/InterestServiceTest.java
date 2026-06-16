package com.bank.application.service;

import com.bank.domain.model.CurrentAccount;
import com.bank.domain.model.Money;
import com.bank.domain.model.SavingsAccount;
import com.bank.support.Fakes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InterestServiceTest {

    private Fakes.InMemoryAccountRepository accounts;
    private Fakes.InMemoryTransactionRepository transactions;
    private InterestService service;

    @BeforeEach
    void setUp() {
        accounts = new Fakes.InMemoryAccountRepository();
        transactions = new Fakes.InMemoryTransactionRepository();
        service = new InterestService(accounts, transactions,
                new Fakes.SequentialIdGenerator(), new Fakes.FixedClock(LocalDate.of(2026, 1, 1)));
    }

    // IS1 : credite un compte epargne actif et enregistre une ecriture INTEREST
    @Test
    void capitalize_activeSavings_credits() {
        accounts.save(new SavingsAccount("s1", "c1", Money.of(120000), 0.12)); // 1% mensuel -> 1200
        int credited = service.capitalizeSavings();
        assertThat(credited).isEqualTo(1);
        assertThat(accounts.findById("s1").orElseThrow().balance().amount()).isEqualTo(121200);
        assertThat(transactions.countByAccountId("s1")).isEqualTo(1);
    }

    // IS2 : ignore les comptes courants
    @Test
    void capitalize_currentAccount_skipped() {
        accounts.save(new CurrentAccount("a1", "c1", Money.of(120000), 0));
        assertThat(service.capitalizeSavings()).isZero();
        assertThat(transactions.count()).isZero();
    }

    // IS3 : ignore une epargne gelee
    @Test
    void capitalize_frozenSavings_skipped() {
        SavingsAccount s = new SavingsAccount("s1", "c1", Money.of(120000), 0.12);
        s.freeze();
        accounts.save(s);
        assertThat(service.capitalizeSavings()).isZero();
        assertThat(accounts.findById("s1").orElseThrow().balance().amount()).isEqualTo(120000);
    }

    // IS4 : taux nul -> interet nul -> aucun credit
    @Test
    void capitalize_zeroRate_skipped() {
        accounts.save(new SavingsAccount("s1", "c1", Money.of(120000), 0.0));
        assertThat(service.capitalizeSavings()).isZero();
        assertThat(transactions.count()).isZero();
    }

    // IS5 : aucun compte -> 0
    @Test
    void capitalize_noAccounts_returnsZero() {
        assertThat(service.capitalizeSavings()).isZero();
    }
}
