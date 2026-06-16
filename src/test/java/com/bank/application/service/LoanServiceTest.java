package com.bank.application.service;

import com.bank.domain.exception.AccountNotFoundException;
import com.bank.domain.exception.ClientNotFoundException;
import com.bank.domain.exception.ForbiddenException;
import com.bank.domain.exception.InvalidAmountException;
import com.bank.domain.exception.LoanAlreadyClosedException;
import com.bank.domain.exception.LoanNotFoundException;
import com.bank.domain.model.Client;
import com.bank.domain.model.CurrentAccount;
import com.bank.domain.model.Loan;
import com.bank.domain.model.LoanStatus;
import com.bank.domain.model.Money;
import com.bank.support.Fakes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanServiceTest {

    private Fakes.InMemoryLoanRepository loans;
    private Fakes.InMemoryClientRepository clients;
    private Fakes.InMemoryAccountRepository accounts;
    private Fakes.InMemoryTransactionRepository transactions;
    private LoanService service;

    @BeforeEach
    void setUp() {
        loans = new Fakes.InMemoryLoanRepository();
        clients = new Fakes.InMemoryClientRepository();
        accounts = new Fakes.InMemoryAccountRepository();
        transactions = new Fakes.InMemoryTransactionRepository();
        service = new LoanService(loans, clients, accounts, transactions,
                new Fakes.SequentialIdGenerator(), new Fakes.FixedClock(LocalDate.of(2026, 1, 1)));
        clients.save(new Client("c1", "John", "Doe"));
        accounts.save(new CurrentAccount("a1", "c1", Money.of(0), 1_000_000));
    }

    private Loan request() {
        return service.requestLoan("c1", "a1", 120000, 0.12, 12);
    }

    // LS1
    @Test
    void request_unknownClient_throws() {
        assertThatThrownBy(() -> service.requestLoan("nope", "a1", 120000, 0.12, 12))
                .isInstanceOf(ClientNotFoundException.class);
    }

    // LS2
    @Test
    void request_unknownAccount_throws() {
        assertThatThrownBy(() -> service.requestLoan("c1", "nope", 120000, 0.12, 12))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // LS2b : compte n'appartenant pas au client -> 403
    @Test
    void request_accountNotOwnedByClient_forbidden() {
        accounts.save(new CurrentAccount("a2", "other", Money.of(0), 1_000_000));
        assertThatThrownBy(() -> service.requestLoan("c1", "a2", 120000, 0.12, 12))
                .isInstanceOf(ForbiddenException.class);
    }

    // LS3
    @Test
    void request_valid_disbursesAndSchedules() {
        Loan loan = request();
        assertThat(loan.schedule()).hasSize(12);
        assertThat(accounts.findById("a1").orElseThrow().balance().amount()).isEqualTo(120000);
        assertThat(transactions.findByAccountId("a1")).hasSize(1);
    }

    // LS4
    @Test
    void repay_invalidAmount_throws() {
        Loan loan = request();
        assertThatThrownBy(() -> service.repayLoan(loan.id(), 0))
                .isInstanceOf(InvalidAmountException.class);
    }

    // LS5
    @Test
    void repay_unknownLoan_throws() {
        assertThatThrownBy(() -> service.repayLoan("nope", 100))
                .isInstanceOf(LoanNotFoundException.class);
    }

    // LS6
    @Test
    void repay_paidOffLoan_throws() {
        Loan loan = request();
        service.repayLoan(loan.id(), 120000); // solde
        assertThatThrownBy(() -> service.repayLoan(loan.id(), 1))
                .isInstanceOf(LoanAlreadyClosedException.class);
    }

    // LS7
    @Test
    void repay_valid_reducesOutstanding() {
        Loan loan = request();
        service.repayLoan(loan.id(), 20000);
        assertThat(loan.outstandingPrincipal().amount()).isEqualTo(100000);
    }

    // LS7c : sur-paiement borne au capital du ; le compte n'est debite que du reel
    @Test
    void repay_overpayment_clampedToOutstanding() {
        Loan loan = request();
        service.repayLoan(loan.id(), 500000);
        assertThat(loan.outstandingPrincipal().amount()).isZero();
        assertThat(loan.status()).isEqualTo(LoanStatus.PAID_OFF);
        assertThat(accounts.findById("a1").orElseThrow().balance().amount()).isZero();
    }

    // LS7b : pret referencant un compte absent -> 404 (chemin defensif)
    @Test
    void repay_loanWithMissingAccount_throws() {
        Loan ghost = Loan.restore("l9", "c1", "ghost", Money.of(1000), 0.0, 2,
                LocalDate.of(2026, 1, 1), Money.of(1000), LoanStatus.ACTIVE, List.of());
        loans.save(ghost);
        assertThatThrownBy(() -> service.repayLoan("l9", 100))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // LS8
    @Test
    void schedule_unknown_throws() {
        assertThatThrownBy(() -> service.getSchedule("nope"))
                .isInstanceOf(LoanNotFoundException.class);
    }

    // LS9
    @Test
    void schedule_existing_returns() {
        Loan loan = request();
        assertThat(service.getSchedule(loan.id())).hasSize(12);
    }

    // listing admin
    @Test
    void listLoans_returnsPage() {
        request();
        request();
        var page = service.listLoans(0, 1);
        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(2);
    }

    // listing par client : client absent
    @Test
    void listClientLoans_unknownClient_throws() {
        assertThatThrownBy(() -> service.listClientLoans("nope", 0, 20))
                .isInstanceOf(ClientNotFoundException.class);
    }

    // listing par client : ok
    @Test
    void listClientLoans_existing_returnsPage() {
        request();
        var page = service.listClientLoans("c1", 0, 20);
        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
    }
}
