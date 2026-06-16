package com.bank.application.service;

import com.bank.domain.exception.AccountNotFoundException;
import com.bank.domain.exception.ClientNotFoundException;
import com.bank.domain.exception.InvalidAmountException;
import com.bank.domain.model.Account;
import com.bank.domain.model.AccountType;
import com.bank.domain.model.Client;
import com.bank.support.Fakes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountServiceTest {

    private Fakes.InMemoryAccountRepository accounts;
    private Fakes.InMemoryClientRepository clients;
    private Fakes.InMemoryTransactionRepository transactions;
    private AccountService service;

    @BeforeEach
    void setUp() {
        accounts = new Fakes.InMemoryAccountRepository();
        clients = new Fakes.InMemoryClientRepository();
        transactions = new Fakes.InMemoryTransactionRepository();
        service = new AccountService(accounts, clients, transactions,
                new Fakes.SequentialIdGenerator(), new Fakes.FixedClock(LocalDate.of(2026, 1, 1)));
        clients.save(new Client("c1", "John", "Doe"));
    }

    private Account openCurrent() {
        return service.openAccount("c1", AccountType.CURRENT, 0, 0.0);
    }

    // AC1
    @Test
    void open_unknownClient_throws() {
        assertThatThrownBy(() -> service.openAccount("nope", AccountType.CURRENT, 0, 0.0))
                .isInstanceOf(ClientNotFoundException.class);
    }

    // AC2
    @Test
    void open_current_ok() {
        Account a = service.openAccount("c1", AccountType.CURRENT, 500, 0.0);
        assertThat(a.type()).isEqualTo(AccountType.CURRENT);
    }

    // AC3
    @Test
    void open_savings_ok() {
        Account a = service.openAccount("c1", AccountType.SAVINGS, 0, 0.03);
        assertThat(a.type()).isEqualTo(AccountType.SAVINGS);
    }

    // AC4
    @Test
    void open_negativeOverdraft_throws() {
        assertThatThrownBy(() -> service.openAccount("c1", AccountType.CURRENT, -1, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // AC5
    @Test
    void open_negativeRate_throws() {
        assertThatThrownBy(() -> service.openAccount("c1", AccountType.SAVINGS, 0, -0.01))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // AC6
    @Test
    void get_unknown_throws() {
        assertThatThrownBy(() -> service.getAccount("nope"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // AC7
    @Test
    void get_existing_returns() {
        Account a = openCurrent();
        assertThat(service.getAccount(a.id())).isSameAs(a);
    }

    // AC8
    @Test
    void deposit_invalidAmount_throws() {
        Account a = openCurrent();
        assertThatThrownBy(() -> service.deposit(a.id(), 0))
                .isInstanceOf(InvalidAmountException.class);
    }

    // AC9
    @Test
    void deposit_unknownAccount_throws() {
        assertThatThrownBy(() -> service.deposit("nope", 100))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // AC10
    @Test
    void deposit_valid_recordsTxn() {
        Account a = openCurrent();
        Account updated = service.deposit(a.id(), 100);
        assertThat(updated.balance().amount()).isEqualTo(100);
        assertThat(transactions.findByAccountId(a.id())).hasSize(1);
    }

    // AC11
    @Test
    void withdraw_invalidAmount_throws() {
        Account a = openCurrent();
        assertThatThrownBy(() -> service.withdraw(a.id(), -5))
                .isInstanceOf(InvalidAmountException.class);
    }

    // AC12
    @Test
    void withdraw_unknownAccount_throws() {
        assertThatThrownBy(() -> service.withdraw("nope", 100))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // AC13
    @Test
    void withdraw_valid_recordsTxn() {
        Account a = service.openAccount("c1", AccountType.CURRENT, 1000, 0.0);
        service.deposit(a.id(), 500);
        Account updated = service.withdraw(a.id(), 200);
        assertThat(updated.balance().amount()).isEqualTo(300);
    }

    // AC14
    @Test
    void history_unknown_throws() {
        assertThatThrownBy(() -> service.getHistory("nope"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // AC15
    @Test
    void history_existing_returnsList() {
        Account a = openCurrent();
        service.deposit(a.id(), 100);
        assertThat(service.getHistory(a.id())).hasSize(1);
    }
}
