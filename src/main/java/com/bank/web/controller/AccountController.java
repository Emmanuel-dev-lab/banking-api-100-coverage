package com.bank.web.controller;

import com.bank.application.service.AccountService;
import com.bank.application.service.AuthService;
import com.bank.application.service.AuthorizationGuard;
import com.bank.domain.model.Account;
import com.bank.domain.port.TokenClaims;
import com.bank.web.RequestAuth;
import com.bank.web.dto.AccountResponse;
import com.bank.web.dto.AmountRequest;
import com.bank.web.dto.PageResponse;
import com.bank.web.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/accounts")
@Tag(name = "Comptes", description = "Consultation, depots, retraits et historique")
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

    @GetMapping
    @Operation(summary = "Lister tous les comptes", description = "Reserve aux ADMIN. Pagine via page/size.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de comptes"),
            @ApiResponse(responseCode = "400", description = "Parametres de pagination invalides"),
            @ApiResponse(responseCode = "403", description = "Role ADMIN requis")
    })
    public ResponseEntity<PageResponse<AccountResponse>> list(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        guard.requireAdmin(authService.authenticate(RequestAuth.bearer(authorization)));
        return ResponseEntity.ok(PageResponse.of(accountService.listAccounts(page, size), AccountResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un compte", description = "Renvoie le solde et le statut du compte.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte trouve"),
            @ApiResponse(responseCode = "403", description = "Acces au compte d'autrui interdit"),
            @ApiResponse(responseCode = "404", description = "Compte inconnu")
    })
    public ResponseEntity<AccountResponse> getAccount(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        Account account = authorize(authorization, id);
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @PostMapping("/{id}/deposit")
    @Operation(summary = "Deposer", description = "Credite le compte d'un montant strictement positif (XAF entier).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Depot effectue, nouveau solde renvoye"),
            @ApiResponse(responseCode = "400", description = "Montant <= 0"),
            @ApiResponse(responseCode = "409", description = "Compte gele ou ferme"),
            @ApiResponse(responseCode = "404", description = "Compte inconnu")
    })
    public ResponseEntity<AccountResponse> deposit(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id,
            @jakarta.validation.Valid @RequestBody AmountRequest request) {
        authorize(authorization, id);
        Account account = accountService.deposit(id, request.amount());
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Retirer",
            description = "Debite le compte. Un compte courant autorise le decouvert jusqu'a son plafond ; "
                    + "un compte epargne refuse tout solde negatif.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retrait effectue"),
            @ApiResponse(responseCode = "400", description = "Montant <= 0"),
            @ApiResponse(responseCode = "409", description = "Compte gele ou ferme"),
            @ApiResponse(responseCode = "422", description = "Fonds/decouvert insuffisants"),
            @ApiResponse(responseCode = "404", description = "Compte inconnu")
    })
    public ResponseEntity<AccountResponse> withdraw(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id,
            @jakarta.validation.Valid @RequestBody AmountRequest request) {
        authorize(authorization, id);
        Account account = accountService.withdraw(id, request.amount());
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Historique",
            description = "Liste paginee des ecritures du compte (depots, retraits, virements, prets), "
                    + "des plus recentes aux plus anciennes. Pagine via page/size.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique renvoye"),
            @ApiResponse(responseCode = "400", description = "Parametres de pagination invalides"),
            @ApiResponse(responseCode = "403", description = "Acces au compte d'autrui interdit"),
            @ApiResponse(responseCode = "404", description = "Compte inconnu")
    })
    public ResponseEntity<PageResponse<TransactionResponse>> transactions(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        authorize(authorization, id);
        return ResponseEntity.ok(PageResponse.of(
                accountService.listTransactions(id, page, size), TransactionResponse::from));
    }

    @PostMapping("/{id}/freeze")
    @Operation(summary = "Geler un compte",
            description = "Reserve aux ADMIN. Bloque depots, retraits et virements jusqu'a reactivation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte gele"),
            @ApiResponse(responseCode = "403", description = "Role ADMIN requis"),
            @ApiResponse(responseCode = "404", description = "Compte inconnu")
    })
    public ResponseEntity<AccountResponse> freeze(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        guard.requireAdmin(authService.authenticate(RequestAuth.bearer(authorization)));
        return ResponseEntity.ok(AccountResponse.from(accountService.freeze(id)));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Fermer un compte",
            description = "Reserve aux ADMIN. Refuse si le solde n'est pas a zero. Etat definitif.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte ferme"),
            @ApiResponse(responseCode = "403", description = "Role ADMIN requis"),
            @ApiResponse(responseCode = "404", description = "Compte inconnu"),
            @ApiResponse(responseCode = "409", description = "Solde non nul : compte non soldable")
    })
    public ResponseEntity<AccountResponse> close(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        guard.requireAdmin(authService.authenticate(RequestAuth.bearer(authorization)));
        return ResponseEntity.ok(AccountResponse.from(accountService.close(id)));
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactiver un compte",
            description = "Reserve aux ADMIN. Repasse un compte gele a l'etat actif. Refuse un compte ferme.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte reactive"),
            @ApiResponse(responseCode = "403", description = "Role ADMIN requis"),
            @ApiResponse(responseCode = "404", description = "Compte inconnu"),
            @ApiResponse(responseCode = "409", description = "Compte ferme : reactivation impossible")
    })
    public ResponseEntity<AccountResponse> reactivate(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        guard.requireAdmin(authService.authenticate(RequestAuth.bearer(authorization)));
        return ResponseEntity.ok(AccountResponse.from(accountService.reactivate(id)));
    }

    /** Authentifie, charge le compte et verifie la propriete ; renvoie le compte. */
    private Account authorize(String authorization, String accountId) {
        TokenClaims claims = authService.authenticate(RequestAuth.bearer(authorization));
        Account account = accountService.getAccount(accountId);
        guard.requireOwnerOrAdmin(claims, account.clientId());
        return account;
    }
}
