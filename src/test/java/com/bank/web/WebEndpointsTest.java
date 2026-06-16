package com.bank.web;

import com.bank.application.service.AccountService;
import com.bank.application.service.AuthService;
import com.bank.application.service.AuthorizationGuard;
import com.bank.application.service.ClientService;
import com.bank.application.service.InterestService;
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
import com.bank.web.controller.JobController;
import com.bank.web.controller.LoanController;
import com.bank.web.controller.MeController;
import com.bank.web.controller.TransferController;
import com.bank.web.dto.AccountResponse;
import com.bank.web.dto.AmountRequest;
import com.bank.web.dto.ChangePasswordRequest;
import com.bank.web.dto.CreateClientRequest;
import com.bank.web.dto.CreateLoanRequest;
import com.bank.web.dto.LoginRequest;
import com.bank.web.dto.MeCreateLoanRequest;
import com.bank.web.dto.OpenAccountRequest;
import com.bank.web.dto.UpdateClientRequest;
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
    private MeController meController;
    private JobController jobController;

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

        var authService = new AuthService(users, hasher, tokens, new Fakes.FakeLoginAttemptGuard());
        var guard = new AuthorizationGuard();
        var clientService = new ClientService(clients, users, ids, hasher);
        var accountService = new AccountService(accounts, clients, transactions, ids, clock);
        var transferService = new TransferService(accounts, transactions, ids, clock);
        var loanService = new LoanService(loans, clients, accounts, transactions, ids, clock);
        var interestService = new InterestService(accounts, transactions, ids, clock);

        authController = new AuthController(authService);
        clientController = new ClientController(clientService, accountService, loanService, authService, guard);
        accountController = new AccountController(accountService, authService, guard);
        transferController = new TransferController(accountService, transferService, authService, guard);
        loanController = new LoanController(loanService, authService, guard);
        meController = new MeController(clientService, accountService, loanService, authService, guard);
        jobController = new JobController(interestService, loanService, authService, guard);

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

    // E3b : modifier un client (owner)
    @Test
    void updateClient_owner_200() {
        var response = clientController.update(clientToken, client.id(),
                new UpdateClientRequest("Johnny", "Doe-Smith"));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().firstName()).isEqualTo("Johnny");
        assertThat(response.getBody().lastName()).isEqualTo("Doe-Smith");
    }

    // E3c : changer mon mot de passe (client)
    @Test
    void changePassword_client_204() {
        var response = meController.changePassword(clientToken, new ChangePasswordRequest("pw", "newpw"));
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(authController.login(new LoginRequest("john", "newpw"))
                .getStatusCode().value()).isEqualTo(200);
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
        var response = accountController.transactions(clientToken, accountId, 0, 20);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().content()).hasSize(1);
        assertThat(response.getBody().totalElements()).isEqualTo(1);
    }

    // E7b : geler un compte (ADMIN)
    @Test
    void freeze_admin_200() {
        var response = accountController.freeze(adminToken, accountId);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().status()).isEqualTo("FROZEN");
    }

    // E7c : geler interdit a un client
    @Test
    void freeze_byClient_forbidden() {
        assertThatThrownBy(() -> accountController.freeze(clientToken, accountId))
                .isInstanceOf(ForbiddenException.class);
    }

    // E7d : reactiver un compte gele (ADMIN)
    @Test
    void reactivate_admin_200() {
        accountController.freeze(adminToken, accountId);
        var response = accountController.reactivate(adminToken, accountId);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().status()).isEqualTo("ACTIVE");
    }

    // E7e : fermer un compte solde (ADMIN)
    @Test
    void close_admin_200() {
        AccountResponse fresh = clientController.openAccount(clientToken, client.id(),
                new OpenAccountRequest(AccountType.SAVINGS, 0, 0.03)).getBody();
        var response = accountController.close(adminToken, fresh.id());
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().status()).isEqualTo("CLOSED");
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

    // listing clients (ADMIN)
    @Test
    void listClients_admin_200() {
        var response = clientController.list(adminToken, 0, 20);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().totalElements()).isGreaterThanOrEqualTo(1);
        assertThat(response.getBody().page()).isZero();
    }

    // listing tous comptes (ADMIN)
    @Test
    void listAccounts_admin_200() {
        var response = accountController.list(adminToken, 0, 20);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().totalElements()).isGreaterThanOrEqualTo(1);
    }

    // listing comptes d'un client (owner)
    @Test
    void listClientAccounts_owner_200() {
        var response = clientController.listAccounts(clientToken, client.id(), 0, 20);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().content()).isNotEmpty();
    }

    // GET /me (client)
    @Test
    void me_client_200() {
        var response = meController.me(clientToken);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().id()).isEqualTo(client.id());
    }

    // GET /me par un ADMIN -> 403 (pas de clientId)
    @Test
    void me_admin_forbidden() {
        assertThatThrownBy(() -> meController.me(adminToken))
                .isInstanceOf(ForbiddenException.class);
    }

    // GET /me/accounts
    @Test
    void myAccounts_client_200() {
        var response = meController.myAccounts(clientToken, 0, 20);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().content()).isNotEmpty();
    }

    // GET /me/loans
    @Test
    void myLoans_client_200() {
        meController.requestLoan(clientToken, new MeCreateLoanRequest(accountId, 120000, 0.12, 12));
        var response = meController.myLoans(clientToken, 0, 20);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().totalElements()).isEqualTo(1);
    }

    // POST /me/accounts
    @Test
    void openMyAccount_201() {
        var response = meController.openAccount(clientToken, new OpenAccountRequest(AccountType.SAVINGS, 0, 0.03));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().type()).isEqualTo("SAVINGS");
    }

    // POST /me/loans
    @Test
    void requestMyLoan_201() {
        var response = meController.requestLoan(clientToken, new MeCreateLoanRequest(accountId, 120000, 0.12, 12));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().schedule()).hasSize(12);
        assertThat(response.getBody().clientId()).isEqualTo(client.id());
    }

    // GET /loans (ADMIN)
    @Test
    void listLoans_admin_200() {
        loanController.create(clientToken, new CreateLoanRequest(client.id(), accountId, 120000, 0.12, 12));
        var response = loanController.list(adminToken, 0, 20);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().totalElements()).isEqualTo(1);
    }

    // GET /loans/{id}
    @Test
    void getLoan_owner_200() {
        String loanId = createLoanAndGetId();
        var response = loanController.get(clientToken, loanId);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().id()).isEqualTo(loanId);
        assertThat(response.getBody().status()).isEqualTo("ACTIVE");
    }

    // GET /clients/{id}/loans (owner)
    @Test
    void listClientLoans_owner_200() {
        loanController.create(clientToken, new CreateLoanRequest(client.id(), accountId, 120000, 0.12, 12));
        var response = clientController.listLoans(clientToken, client.id(), 0, 20);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().totalElements()).isEqualTo(1);
    }

    // accesseur compte (couvre GET /accounts/{id})
    @Test
    void getAccount_owner_200() {
        var response = accountController.getAccount(clientToken, accountId);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().id()).isEqualTo(accountId);
    }

    // J1 : capitalisation des interets (ADMIN)
    @Test
    void capitalizeInterest_admin_200() {
        meController.openAccount(clientToken, new OpenAccountRequest(AccountType.SAVINGS, 0, 0.12));
        accountController.deposit(adminToken,
                meController.myAccounts(clientToken, 0, 20).getBody().content().stream()
                        .filter(a -> a.type().equals("SAVINGS")).findFirst().orElseThrow().id(),
                new AmountRequest(120000));
        var response = jobController.capitalizeInterest(adminToken);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().processed()).isEqualTo(1);
    }

    // J2 : capitalisation interdite a un client
    @Test
    void capitalizeInterest_byClient_forbidden() {
        assertThatThrownBy(() -> jobController.capitalizeInterest(clientToken))
                .isInstanceOf(ForbiddenException.class);
    }

    // J3 : marquage des prets en retard (ADMIN)
    @Test
    void flagOverdue_admin_200() {
        var response = jobController.flagOverdue(adminToken);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().processed()).isZero();
    }

    // J4 : marquage interdit a un client
    @Test
    void flagOverdue_byClient_forbidden() {
        assertThatThrownBy(() -> jobController.flagOverdue(clientToken))
                .isInstanceOf(ForbiddenException.class);
    }

    private String createLoanAndGetId() {
        return loanController.create(clientToken,
                new CreateLoanRequest(client.id(), accountId, 120000, 0.12, 12))
                .getBody().id();
    }
}
