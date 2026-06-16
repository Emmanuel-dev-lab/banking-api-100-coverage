package com.bank.web.controller;

import com.bank.application.service.AccountService;
import com.bank.application.service.AuthService;
import com.bank.application.service.AuthorizationGuard;
import com.bank.application.service.ClientService;
import com.bank.application.service.LoanService;
import com.bank.domain.model.Account;
import com.bank.domain.model.Loan;
import com.bank.domain.port.TokenClaims;
import com.bank.web.RequestAuth;
import com.bank.web.dto.AccountResponse;
import com.bank.web.dto.ChangePasswordRequest;
import com.bank.web.dto.ClientResponse;
import com.bank.web.dto.LoanResponse;
import com.bank.web.dto.MeCreateLoanRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints self-service du client courant. L'identite vient du jeton
 * (jamais d'id dans l'URL) ; un ADMIN n'a pas de clientId -> 403.
 */
@RestController
@RequestMapping("/api/me")
@Tag(name = "Moi (self-service)", description = "Ressources du client connecte, identifie par son jeton")
public class MeController {

    private final ClientService clientService;
    private final AccountService accountService;
    private final LoanService loanService;
    private final AuthService authService;
    private final AuthorizationGuard guard;

    public MeController(ClientService clientService, AccountService accountService, LoanService loanService,
                        AuthService authService, AuthorizationGuard guard) {
        this.clientService = clientService;
        this.accountService = accountService;
        this.loanService = loanService;
        this.authService = authService;
        this.guard = guard;
    }

    @GetMapping
    @Operation(summary = "Mon profil", description = "Renvoie le client correspondant au jeton.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil du client"),
            @ApiResponse(responseCode = "401", description = "Jeton absent ou invalide"),
            @ApiResponse(responseCode = "403", description = "Compte client requis (un ADMIN n'a pas de profil client)")
    })
    public ResponseEntity<ClientResponse> me(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization) {
        String clientId = guard.requireClientId(authenticate(authorization));
        return ResponseEntity.ok(ClientResponse.from(clientService.getClient(clientId)));
    }

    @GetMapping("/accounts")
    @Operation(summary = "Mes comptes", description = "Liste paginee des comptes du client courant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de comptes"),
            @ApiResponse(responseCode = "400", description = "Parametres de pagination invalides"),
            @ApiResponse(responseCode = "403", description = "Compte client requis")
    })
    public ResponseEntity<PageResponse<AccountResponse>> myAccounts(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String clientId = guard.requireClientId(authenticate(authorization));
        return ResponseEntity.ok(PageResponse.of(
                accountService.listClientAccounts(clientId, page, size), AccountResponse::from));
    }

    @GetMapping("/loans")
    @Operation(summary = "Mes prets", description = "Liste paginee des prets du client courant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de prets"),
            @ApiResponse(responseCode = "400", description = "Parametres de pagination invalides"),
            @ApiResponse(responseCode = "403", description = "Compte client requis")
    })
    public ResponseEntity<PageResponse<LoanResponse>> myLoans(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String clientId = guard.requireClientId(authenticate(authorization));
        return ResponseEntity.ok(PageResponse.of(
                loanService.listClientLoans(clientId, page, size), LoanResponse::from));
    }

    @PostMapping("/accounts")
    @Operation(summary = "Ouvrir un de mes comptes", description = "Ouvre un compte pour le client courant.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte ouvert"),
            @ApiResponse(responseCode = "400", description = "Plafond/taux negatif"),
            @ApiResponse(responseCode = "403", description = "Compte client requis")
    })
    public ResponseEntity<AccountResponse> openAccount(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @jakarta.validation.Valid @RequestBody OpenAccountRequest request) {
        String clientId = guard.requireClientId(authenticate(authorization));
        Account account = accountService.openAccount(
                clientId, request.type(), request.overdraftLimit(), request.annualRate());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @PostMapping("/loans")
    @Operation(summary = "Demander un de mes prets", description = "Cree un pret pour le client courant.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pret cree"),
            @ApiResponse(responseCode = "400", description = "Termes invalides"),
            @ApiResponse(responseCode = "403", description = "Compte client requis"),
            @ApiResponse(responseCode = "404", description = "Compte inconnu")
    })
    public ResponseEntity<LoanResponse> requestLoan(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @jakarta.validation.Valid @RequestBody MeCreateLoanRequest request) {
        String clientId = guard.requireClientId(authenticate(authorization));
        Loan loan = loanService.requestLoan(clientId, request.accountId(),
                request.principal(), request.annualRate(), request.termMonths());
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanResponse.from(loan));
    }

    @PostMapping("/password")
    @Operation(summary = "Changer mon mot de passe",
            description = "Verifie l'ancien mot de passe et le remplace par le nouveau.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mot de passe change"),
            @ApiResponse(responseCode = "400", description = "Champ manquant"),
            @ApiResponse(responseCode = "401", description = "Jeton ou ancien mot de passe invalide")
    })
    public ResponseEntity<Void> changePassword(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @jakarta.validation.Valid @RequestBody ChangePasswordRequest request) {
        TokenClaims claims = authenticate(authorization);
        authService.changePassword(claims.userId(), request.oldPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private TokenClaims authenticate(String authorization) {
        return authService.authenticate(RequestAuth.bearer(authorization));
    }
}
