package com.bank.web;

import com.bank.application.service.AccountService;
import com.bank.application.service.AuthService;
import com.bank.application.service.AuthorizationGuard;
import com.bank.application.service.ClientService;
import com.bank.application.service.LoanService;
import com.bank.application.service.TransferService;
import com.bank.domain.exception.ForbiddenException;
import com.bank.domain.exception.UnauthorizedException;
import com.bank.domain.model.AccountType;
import com.bank.domain.model.Client;
import com.bank.domain.model.Role;
import com.bank.domain.model.User;
import com.bank.support.Fakes;
import com.bank.web.controller.AccountController;
import com.bank.web.controller.AuthController;
import com.bank.web.controller.ClientController;
import com.bank.web.controller.LoanController;
import com.bank.web.controller.TransferController;
import com.bank.web.dto.AccountResponse;
import com.bank.web.dto.AmountRequest;
import com.bank.web.dto.CreateClientRequest;
import com.bank.web.dto.CreateLoanRequest;
import com.bank.web.dto.LoginRequest;
import com.bank.web.dto.OpenAccountRequest;
import com.bank.web.dto.TransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebEndpointsTest {

    private Fakes.InMemoryUserRepository users;
    private Fakes.FakeTokenService tokens;

    private AuthController authController;
    private ClientController clientController;
    private AccountController accountController;
    private TransferController transferController;
    private LoanController loanController;

    private Client client;
    private String adminToken;
    private String clientToken;
    private String accountId;

    @BeforeEach
    void setUp() {
        var clients = new Fakes.InMemoryClientRepository();
        var accounts = new Fakes.InMemoryAccountRepository();
        var transactions = new Fakes.InMemoryTransactionRepository();
        var loans = new Fakes.InMemoryLoanRepository();
        users = new Fakes.InMemoryUserRepository();
        var ids = new Fakes.SequentialIdGenerator();
        var clock = new Fakes.FixedClock(LocalDate.of(2026, 1, 1));
        var hasher = new Fakes.FakePasswordHasher();
        tokens = new Fakes.FakeTokenService();

        var authService = new AuthService(users, hasher, tokens);
        var guard = new AuthorizationGuard();
        var clientService = new ClientService(clients, users, ids, hasher);
        var accountService = new AccountService(accounts, clients, transactions, ids, clock);
        var transferService = new TransferService(accounts, transactions, ids, clock);
        var loanService = new LoanService(loans, clients, accounts, transactions, ids, clock);

        authController = new AuthController(authService);
        clientController = new ClientController(clientService, accountService, authService, guard);
        accountController = new AccountController(accountService, authService, guard);
        transferController = new TransferController(accountService, transferService, authService, guard);
        loanController = new LoanController(loanService, authService, guard);

        User admin = new User("u-admin", "admin", hasher.hash("pw"), Role.ADMIN, null);
        users.save(admin);
        adminToken = "Bearer " + tokens.issue(admin);

        client = clientService.createClient("John", "Doe", "john", "pw");
        User clientUser = users.findByUsername("john").orElseThrow();
        clientToken = "Bearer " + tokens.issue(clientUser);

        accountId = clientController.openAccount(clientToken, client.id(),
                new OpenAccountRequest(AccountType.CURRENT, 1_000_000, 0.0)).getBody().id();
    }

    // E1
    @Test
    void login_returnsToken() {
        var response = authController.login(new LoginRequest("john", "pw"));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().token()).isNotBlank();
    }

    // E2
    @Test
    void createClient_admin_201() {
        var response = clientController.create(adminToken,
                new CreateClientRequest("Alice", "Smith", "alice", "pw"));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().firstName()).isEqualTo("Alice");
    }

    // E3
    @Test
    void getClient_owner_200() {
        var response = clientController.get(clientToken, client.id());
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().id()).isEqualTo(client.id());
    }

    // E4
    @Test
    void openAccount_201() {
        var response = clientController.openAccount(clientToken, client.id(),
                new OpenAccountRequest(AccountType.SAVINGS, 0, 0.03));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().type()).isEqualTo("SAVINGS");
    }

    // E5
    @Test
    void deposit_200() {
        var response = accountController.deposit(clientToken, accountId, new AmountRequest(100));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().balance()).isEqualTo(100);
    }

    // E6
    @Test
    void withdraw_200() {
        var response = accountController.withdraw(clientToken, accountId, new AmountRequest(40));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().balance()).isEqualTo(-40);
    }

    // E7
    @Test
    void transactions_200() {
        accountController.deposit(clientToken, accountId, new AmountRequest(100));
        var response = accountController.transactions(clientToken, accountId);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
    }

    // E8
    @Test
    void transfer_200() {
        AccountResponse dest = clientController.openAccount(clientToken, client.id(),
                new OpenAccountRequest(AccountType.CURRENT, 0, 0.0)).getBody();
        accountController.deposit(clientToken, accountId, new AmountRequest(500));
        var response = transferController.transfer(clientToken,
                new TransferRequest(accountId, dest.id(), 300));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    // E9
    @Test
    void createLoan_201() {
        var response = loanController.create(clientToken,
                new CreateLoanRequest(client.id(), accountId, 120000, 0.12, 12));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().schedule()).hasSize(12);
    }

    // E10
    @Test
    void schedule_200() {
        String loanId = createLoanAndGetId();
        var response = loanController.schedule(clientToken, loanId);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(12);
    }

    // E11
    @Test
    void repay_200() {
        String loanId = createLoanAndGetId();
        var response = loanController.repay(clientToken, loanId, new AmountRequest(20000));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    // E12
    @Test
    void protectedEndpoint_noToken_unauthorized() {
        assertThatThrownBy(() -> accountController.getAccount(null, accountId))
                .isInstanceOf(UnauthorizedException.class);
    }

    // E13
    @Test
    void adminEndpoint_byClient_forbidden() {
        assertThatThrownBy(() -> clientController.create(clientToken,
                new CreateClientRequest("X", "Y", "x", "pw")))
                .isInstanceOf(ForbiddenException.class);
    }

    // accesseur compte (couvre GET /accounts/{id})
    @Test
    void getAccount_owner_200() {
        var response = accountController.getAccount(clientToken, accountId);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().id()).isEqualTo(accountId);
    }

    private String createLoanAndGetId() {
        return loanController.create(clientToken,
                new CreateLoanRequest(client.id(), accountId, 120000, 0.12, 12))
                .getBody().id();
    }
}
