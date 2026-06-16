package com.bank.web.controller;

import com.bank.application.service.AccountService;
import com.bank.application.service.AuthService;
import com.bank.application.service.AuthorizationGuard;
import com.bank.application.service.TransferService;
import com.bank.domain.model.Account;
import com.bank.domain.port.TokenClaims;
import com.bank.web.RequestAuth;
import com.bank.web.dto.TransferRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final AccountService accountService;
    private final TransferService transferService;
    private final AuthService authService;
    private final AuthorizationGuard guard;

    public TransferController(AccountService accountService, TransferService transferService,
                             AuthService authService, AuthorizationGuard guard) {
        this.accountService = accountService;
        this.transferService = transferService;
        this.authService = authService;
        this.guard = guard;
    }

    @PostMapping
    public ResponseEntity<Void> transfer(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody TransferRequest request) {
        TokenClaims claims = authService.authenticate(RequestAuth.bearer(authorization));
        Account source = accountService.getAccount(request.sourceAccountId());
        guard.requireOwnerOrAdmin(claims, source.clientId());
        transferService.transfer(request.sourceAccountId(), request.destAccountId(), request.amount());
        return ResponseEntity.ok().build();
    }
}
