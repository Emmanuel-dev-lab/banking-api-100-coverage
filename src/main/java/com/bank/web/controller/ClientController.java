package com.bank.web.controller;

import com.bank.application.service.AccountService;
import com.bank.application.service.AuthService;
import com.bank.application.service.AuthorizationGuard;
import com.bank.application.service.ClientService;
import com.bank.application.service.LoanService;
import com.bank.domain.model.Account;
import com.bank.domain.model.Client;
import com.bank.domain.port.TokenClaims;
import com.bank.web.RequestAuth;
import com.bank.web.dto.AccountResponse;
import com.bank.web.dto.ClientResponse;
import com.bank.web.dto.CreateClientRequest;
import com.bank.web.dto.LoanResponse;
import com.bank.web.dto.OpenAccountRequest;
import com.bank.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Gestion des clients et ouverture de comptes")
public class ClientController {

    private final ClientService clientService;
    private final AccountService accountService;
    private final LoanService loanService;
    private final AuthService authService;
    private final AuthorizationGuard guard;

    public ClientController(ClientService clientService, AccountService accountService, LoanService loanService,
                            AuthService authService, AuthorizationGuard guard) {
        this.clientService = clientService;
        this.accountService = accountService;
        this.loanService = loanService;
        this.authService = authService;
        this.guard = guard;
    }

    @PostMapping
    @Operation(summary = "Creer un client", description = "Reserve aux ADMIN. Cree le client "
            + "et son compte utilisateur CLIENT associe.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client cree"),
            @ApiResponse(responseCode = "401", description = "Jeton absent ou invalide"),
            @ApiResponse(responseCode = "403", description = "Role ADMIN requis")
    })
    public ResponseEntity<ClientResponse> create(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @jakarta.validation.Valid @RequestBody CreateClientRequest request) {
        TokenClaims claims = authService.authenticate(RequestAuth.bearer(authorization));
        guard.requireAdmin(claims);
        Client client = clientService.createClient(
                request.firstName(), request.lastName(), request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(ClientResponse.from(client));
    }

    @GetMapping
    @Operation(summary = "Lister les clients", description = "Reserve aux ADMIN. Pagine via page/size.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de clients"),
            @ApiResponse(responseCode = "400", description = "Parametres de pagination invalides"),
            @ApiResponse(responseCode = "403", description = "Role ADMIN requis")
    })
    public ResponseEntity<PageResponse<ClientResponse>> list(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        guard.requireAdmin(authService.authenticate(RequestAuth.bearer(authorization)));
        return ResponseEntity.ok(PageResponse.of(clientService.listClients(page, size), ClientResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un client", description = "Accessible au client proprietaire ou a un ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client trouve"),
            @ApiResponse(responseCode = "403", description = "Acces au client d'autrui interdit"),
            @ApiResponse(responseCode = "404", description = "Client inconnu")
    })
    public ResponseEntity<ClientResponse> get(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        TokenClaims claims = authService.authenticate(RequestAuth.bearer(authorization));
        Client client = clientService.getClient(id);
        guard.requireOwnerOrAdmin(claims, client.id());
        return ResponseEntity.ok(ClientResponse.from(client));
    }

    @PostMapping("/{clientId}/accounts")
    @Operation(summary = "Ouvrir un compte",
            description = "Ouvre un compte CURRENT (plafond de decouvert) ou SAVINGS (taux d'interet) "
                    + "pour le client. Le solde initial est 0.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte ouvert"),
            @ApiResponse(responseCode = "400", description = "Plafond/taux negatif"),
            @ApiResponse(responseCode = "403", description = "Acces interdit"),
            @ApiResponse(responseCode = "404", description = "Client inconnu")
    })
    public ResponseEntity<AccountResponse> openAccount(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String clientId,
            @jakarta.validation.Valid @RequestBody OpenAccountRequest request) {
        TokenClaims claims = authService.authenticate(RequestAuth.bearer(authorization));
        guard.requireOwnerOrAdmin(claims, clientId);
        Account account = accountService.openAccount(
                clientId, request.type(), request.overdraftLimit(), request.annualRate());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping("/{clientId}/accounts")
    @Operation(summary = "Lister les comptes d'un client",
            description = "Accessible au client proprietaire ou a un ADMIN. Pagine via page/size.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de comptes"),
            @ApiResponse(responseCode = "400", description = "Parametres de pagination invalides"),
            @ApiResponse(responseCode = "403", description = "Acces interdit"),
            @ApiResponse(responseCode = "404", description = "Client inconnu")
    })
    public ResponseEntity<PageResponse<AccountResponse>> listAccounts(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        guard.requireOwnerOrAdmin(authService.authenticate(RequestAuth.bearer(authorization)), clientId);
        return ResponseEntity.ok(PageResponse.of(
                accountService.listClientAccounts(clientId, page, size), AccountResponse::from));
    }

    @GetMapping("/{clientId}/loans")
    @Operation(summary = "Lister les prets d'un client",
            description = "Accessible au client proprietaire ou a un ADMIN. Pagine via page/size.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de prets"),
            @ApiResponse(responseCode = "400", description = "Parametres de pagination invalides"),
            @ApiResponse(responseCode = "403", description = "Acces interdit"),
            @ApiResponse(responseCode = "404", description = "Client inconnu")
    })
    public ResponseEntity<PageResponse<LoanResponse>> listLoans(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        guard.requireOwnerOrAdmin(authService.authenticate(RequestAuth.bearer(authorization)), clientId);
        return ResponseEntity.ok(PageResponse.of(
                loanService.listClientLoans(clientId, page, size), LoanResponse::from));
    }
}
