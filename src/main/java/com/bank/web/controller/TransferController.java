package com.bank.web.controller;

import com.bank.application.service.AccountService;
import com.bank.application.service.AuthService;
import com.bank.application.service.AuthorizationGuard;
import com.bank.application.service.TransferService;
import com.bank.domain.model.Account;
import com.bank.domain.port.TokenClaims;
import com.bank.web.RequestAuth;
import com.bank.web.dto.TransferRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Virements", description = "Transfert atomique entre deux comptes")
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
    @Operation(summary = "Effectuer un virement",
            description = "Debite le compte source et credite le compte destination de maniere atomique. "
                    + "Le demandeur doit etre proprietaire du compte source (ou ADMIN).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Virement effectue"),
            @ApiResponse(responseCode = "400", description = "Montant <= 0"),
            @ApiResponse(responseCode = "403", description = "Compte source non possede"),
            @ApiResponse(responseCode = "404", description = "Compte source ou destination inconnu"),
            @ApiResponse(responseCode = "409", description = "Compte gele ou ferme"),
            @ApiResponse(responseCode = "422", description = "Meme compte source/destination ou fonds insuffisants")
    })
    public ResponseEntity<Void> transfer(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody TransferRequest request) {
        TokenClaims claims = authService.authenticate(RequestAuth.bearer(authorization));
        Account source = accountService.getAccount(request.sourceAccountId());
        guard.requireOwnerOrAdmin(claims, source.clientId());
        transferService.transfer(request.sourceAccountId(), request.destAccountId(), request.amount());
        return ResponseEntity.ok().build();
    }
}
