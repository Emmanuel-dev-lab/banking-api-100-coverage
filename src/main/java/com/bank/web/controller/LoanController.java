package com.bank.web.controller;

import com.bank.application.service.AuthService;
import com.bank.application.service.AuthorizationGuard;
import com.bank.application.service.LoanService;
import com.bank.domain.model.Loan;
import com.bank.domain.port.TokenClaims;
import com.bank.web.RequestAuth;
import com.bank.web.dto.AmountRequest;
import com.bank.web.dto.CreateLoanRequest;
import com.bank.web.dto.InstallmentResponse;
import com.bank.web.dto.LoanResponse;
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

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@Tag(name = "Prets", description = "Demande de pret, echeancier d'amortissement et remboursement")
public class LoanController {

    private final LoanService loanService;
    private final AuthService authService;
    private final AuthorizationGuard guard;

    public LoanController(LoanService loanService, AuthService authService, AuthorizationGuard guard) {
        this.loanService = loanService;
        this.authService = authService;
        this.guard = guard;
    }

    @PostMapping
    @Operation(summary = "Demander un pret",
            description = "Cree un pret a amortissement constant, genere l'echeancier et decaisse "
                    + "le capital sur le compte indique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pret cree, echeancier renvoye"),
            @ApiResponse(responseCode = "400", description = "Termes invalides (capital/taux/duree)"),
            @ApiResponse(responseCode = "403", description = "Acces interdit"),
            @ApiResponse(responseCode = "404", description = "Client ou compte inconnu")
    })
    public ResponseEntity<LoanResponse> create(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody CreateLoanRequest request) {
        TokenClaims claims = authService.authenticate(RequestAuth.bearer(authorization));
        guard.requireOwnerOrAdmin(claims, request.clientId());
        Loan loan = loanService.requestLoan(request.clientId(), request.accountId(),
                request.principal(), request.annualRate(), request.termMonths());
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanResponse.from(loan));
    }

    @GetMapping
    @Operation(summary = "Lister tous les prets", description = "Reserve aux ADMIN. Pagine via page/size.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de prets"),
            @ApiResponse(responseCode = "400", description = "Parametres de pagination invalides"),
            @ApiResponse(responseCode = "403", description = "Role ADMIN requis")
    })
    public ResponseEntity<PageResponse<LoanResponse>> list(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        guard.requireAdmin(authService.authenticate(RequestAuth.bearer(authorization)));
        return ResponseEntity.ok(PageResponse.of(loanService.listLoans(page, size), LoanResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un pret", description = "Renvoie l'etat du pret (statut, capital restant) et son echeancier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pret trouve"),
            @ApiResponse(responseCode = "403", description = "Acces interdit"),
            @ApiResponse(responseCode = "404", description = "Pret inconnu")
    })
    public ResponseEntity<LoanResponse> get(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        return ResponseEntity.ok(LoanResponse.from(authorize(authorization, id)));
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "Consulter l'echeancier", description = "Renvoie les echeances du pret (capital, interets, statut).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Echeancier renvoye"),
            @ApiResponse(responseCode = "403", description = "Acces interdit"),
            @ApiResponse(responseCode = "404", description = "Pret inconnu")
    })
    public ResponseEntity<List<InstallmentResponse>> schedule(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        Loan loan = authorize(authorization, id);
        List<InstallmentResponse> schedule = loan.schedule().stream()
                .map(InstallmentResponse::from)
                .toList();
        return ResponseEntity.ok(schedule);
    }

    @PostMapping("/{id}/repay")
    @Operation(summary = "Rembourser un pret",
            description = "Reduit le capital restant du et debite le compte. Solde le pret quand le capital atteint 0.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Remboursement effectue"),
            @ApiResponse(responseCode = "400", description = "Montant <= 0"),
            @ApiResponse(responseCode = "403", description = "Acces interdit"),
            @ApiResponse(responseCode = "404", description = "Pret inconnu"),
            @ApiResponse(responseCode = "409", description = "Pret deja solde"),
            @ApiResponse(responseCode = "422", description = "Fonds insuffisants sur le compte")
    })
    public ResponseEntity<Void> repay(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id,
            @RequestBody AmountRequest request) {
        authorize(authorization, id);
        loanService.repayLoan(id, request.amount());
        return ResponseEntity.ok().build();
    }

    private Loan authorize(String authorization, String loanId) {
        TokenClaims claims = authService.authenticate(RequestAuth.bearer(authorization));
        Loan loan = loanService.getLoan(loanId);
        guard.requireOwnerOrAdmin(claims, loan.clientId());
        return loan;
    }
}
