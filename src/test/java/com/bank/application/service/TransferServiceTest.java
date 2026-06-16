package com.bank.application.service;

import com.bank.domain.exception.AccountNotActiveException;
import com.bank.domain.exception.AccountNotFoundException;
import com.bank.domain.exception.InsufficientFundsException;
import com.bank.domain.exception.InvalidAmountException;
import com.bank.domain.exception.SameAccountTransferException;
import com.bank.domain.model.CurrentAccount;
import com.bank.domain.model.Money;
import com.bank.support.Fakes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferServiceTest {

    private Fakes.InMemoryAccountRepository accounts;
    private Fakes.InMemoryTransactionRepository transactions;
    private TransferService service;

    @BeforeEach
    void setUp() {
        accounts = new Fakes.InMemoryAccountRepository();
        transactions = new Fakes.InMemoryTransactionRepository();
        service = new TransferService(accounts, transactions,
                new Fakes.SequentialIdGenerator(), new Fakes.FixedClock(LocalDate.of(2026, 1, 1)));
    }

    private CurrentAccount account(String id, long balance) {
        CurrentAccount a = new CurrentAccount(id, "c1", Money.of(balance), 0);
        accounts.save(a);
        return a;
    }

    // T1
    @Test
    void transfer_invalidAmount_throws() {
        account("a1", 1000);
        account("a2", 0);
        assertThatThrownBy(() -> service.transfer("a1", "a2", 0))
                .isInstanceOf(InvalidAmountException.class);
    }

    // T2
    @Test
    void transfer_sameAccount_throws() {
        account("a1", 1000);
        assertThatThrownBy(() -> service.transfer("a1", "a1", 100))
                .isInstanceOf(SameAccountTransferException.class);
    }

    // T3
    @Test
    void transfer_unknownSource_throws() {
        account("a2", 0);
        assertThatThrownBy(() -> service.transfer("nope", "a2", 100))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // T4
    @Test
    void transfer_unknownDest_throws() {
        account("a1", 1000);
        assertThatThrownBy(() -> service.transfer("a1", "nope", 100))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // T5
    @Test
    void transfer_frozenSource_throws() {
        CurrentAccount src = account("a1", 1000);
        account("a2", 0);
        src.freeze();
        assertThatThrownBy(() -> service.transfer("a1", "a2", 100))
                .isInstanceOf(AccountNotActiveException.class);
    }

    // T6
    @Test
    void transfer_frozenDest_throws() {
        account("a1", 1000);
        CurrentAccount dst = account("a2", 0);
        dst.freeze();
        assertThatThrownBy(() -> service.transfer("a1", "a2", 100))
                .isInstanceOf(AccountNotActiveException.class);
    }

    // T7
    @Test
    void transfer_insufficientFunds_throws() {
        account("a1", 50);
        account("a2", 0);
        assertThatThrownBy(() -> service.transfer("a1", "a2", 100))
                .isInstanceOf(InsufficientFundsException.class);
    }

    // T8
    @Test
    void transfer_valid_movesFundsAndRecordsTwoTxns() {
        CurrentAccount src = account("a1", 1000);
        CurrentAccount dst = account("a2", 0);
        service.transfer("a1", "a2", 300);
        assertThat(src.balance().amount()).isEqualTo(700);
        assertThat(dst.balance().amount()).isEqualTo(300);
        assertThat(transactions.count()).isEqualTo(2);
    }
}
