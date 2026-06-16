package com.bank.web.controller;

import com.bank.application.service.AccountService;
import com.bank.application.service.AuthService;
import com.bank.application.service.AuthorizationGuard;
import com.bank.application.service.ClientService;
import com.bank.domain.model.Account;
import com.bank.domain.model.Client;
import com.bank.domain.port.TokenClaims;
import com.bank.web.RequestAuth;
import com.bank.web.dto.AccountResponse;
import com.bank.web.dto.ClientResponse;
import com.bank.web.dto.CreateClientRequest;
import com.bank.web.dto.OpenAccountRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;
    private final AccountService accountService;
    private final AuthService authService;
    private final AuthorizationGuard guard;

    public ClientController(ClientService clientService, AccountService accountService,
                            AuthService authService, AuthorizationGuard guard) {
        this.clientService = clientService;
        this.accountService = accountService;
        this.authService = authService;
        this.guard = guard;
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody CreateClientRequest request) {
        TokenClaims claims = authService.authenticate(RequestAuth.bearer(authorization));
        guard.requireAdmin(claims);
        Client client = clientService.createClient(
                request.firstName(), request.lastName(), request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(ClientResponse.from(client));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> get(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        TokenClaims claims = authService.authenticate(RequestAuth.bearer(authorization));
        Client client = clientService.getClient(id);
        guard.requireOwnerOrAdmin(claims, client.id());
        return ResponseEntity.ok(ClientResponse.from(client));
    }

    @PostMapping("/{clientId}/accounts")
    public ResponseEntity<AccountResponse> openAccount(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String clientId,
            @RequestBody OpenAccountRequest request) {
        TokenClaims claims = authService.authenticate(RequestAuth.bearer(authorization));
        guard.requireOwnerOrAdmin(claims, clientId);
        Account account = accountService.openAccount(
                clientId, request.type(), request.overdraftLimit(), request.annualRate());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }
}
