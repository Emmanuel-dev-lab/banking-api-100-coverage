package com.bank.integration;

import com.bank.application.service.AccountService;
import com.bank.application.service.AuthService;
import com.bank.application.service.ClientService;
import com.bank.application.service.LoanService;
import com.bank.application.service.TransferService;
import com.bank.domain.model.Account;
import com.bank.domain.model.AccountType;
import com.bank.domain.model.Client;
import com.bank.domain.model.Loan;
import com.bank.domain.model.Role;
import com.bank.domain.port.TokenClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test de bout en bout sur le contexte Spring reel (H2 + JPA).
 * Hors quota de couverture : valide le cablage et le round-trip de persistance.
 */
@SpringBootTest
class BankingApiIntegrationTest {

    @Autowired
    private ClientService clientService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private TransferService transferService;
    @Autowired
    private LoanService loanService;

    @Test
    void fullFlow_persistsAndReloadsAggregates() {
        Client client = clientService.createClient("Jane", "Roe", "jane", "pw");

        String token = authService.login("jane", "pw");
        TokenClaims claims = authService.authenticate(token);
        assertThat(claims.role()).isEqualTo(Role.CLIENT);
        assertThat(claims.clientId()).isEqualTo(client.id());

        Account current = accountService.openAccount(client.id(), AccountType.CURRENT, 1_000_000, 0.0);
        Account savings = accountService.openAccount(client.id(), AccountType.SAVINGS, 0, 0.03);

        accountService.deposit(current.id(), 1000);
        transferService.transfer(current.id(), savings.id(), 500);

        assertThat(accountService.getAccount(savings.id()).balance().amount()).isEqualTo(500);
        assertThat(accountService.getHistory(current.id())).isNotEmpty();

        Loan loan = loanService.requestLoan(client.id(), current.id(), 120000, 0.12, 12);
        loanService.repayLoan(loan.id(), 20000);

        assertThat(loanService.getSchedule(loan.id())).hasSize(12);
        assertThat(loanService.getLoan(loan.id()).outstandingPrincipal().amount()).isEqualTo(100000);
    }
}
