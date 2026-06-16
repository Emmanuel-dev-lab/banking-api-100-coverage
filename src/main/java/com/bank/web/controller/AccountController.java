package com.bank.web.controller;

import com.bank.application.service.AccountService;
import com.bank.application.service.AuthService;
import com.bank.application.service.AuthorizationGuard;
import com.bank.domain.model.Account;
import com.bank.domain.port.TokenClaims;
import com.bank.web.RequestAuth;
import com.bank.web.dto.AccountResponse;
import com.bank.web.dto.AmountRequest;
import com.bank.web.dto.TransactionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AuthService authService;
    private final AuthorizationGuard guard;

    public AccountController(AccountService accountService,
                            AuthService authService, AuthorizationGuard guard) {
        this.accountService = accountService;
        this.authService = authService;
        this.guard = guard;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        Account account = authorize(authorization, id);
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id,
            @RequestBody AmountRequest request) {
        authorize(authorization, id);
        Account account = accountService.deposit(id, request.amount());
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id,
            @RequestBody AmountRequest request) {
        authorize(authorization, id);
        Account account = accountService.withdraw(id, request.amount());
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> transactions(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        authorize(authorization, id);
        List<TransactionResponse> history = accountService.getHistory(id).stream()
                .map(TransactionResponse::from)
                .toList();
        return ResponseEntity.ok(history);
    }

    /** Authentifie, charge le compte et verifie la propriete ; renvoie le compte. */
    private Account authorize(String authorization, String accountId) {
        TokenClaims claims = authService.authenticate(RequestAuth.bearer(authorization));
        Account account = accountService.getAccount(accountId);
        guard.requireOwnerOrAdmin(claims, account.clientId());
        return account;
    }
}
