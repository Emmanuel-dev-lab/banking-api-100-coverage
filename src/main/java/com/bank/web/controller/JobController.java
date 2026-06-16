package com.bank.web.controller;

import com.bank.application.service.AuthService;
import com.bank.application.service.AuthorizationGuard;
import com.bank.application.service.InterestService;
import com.bank.application.service.LoanService;
import com.bank.web.RequestAuth;
import com.bank.web.dto.JobResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Declenchement manuel des traitements periodiques (reserve aux ADMIN).
 * Les memes traitements tournent aussi via un planificateur (cron).
 */
@RestController
@RequestMapping("/api/admin/jobs")
@Tag(name = "Jobs (admin)", description = "Declenchement manuel des traitements periodiques")
public class JobController {

    private final InterestService interestService;
    private final LoanService loanService;
    private final AuthService authService;
    private final AuthorizationGuard guard;

    public JobController(InterestService interestService, LoanService loanService,
                         AuthService authService, AuthorizationGuard guard) {
        this.interestService = interestService;
        this.loanService = loanService;
        this.authService = authService;
        this.guard = guard;
    }

    @PostMapping("/interest")
    @Operation(summary = "Capitaliser les interets epargne",
            description = "Applique un mois d'interets a chaque compte epargne actif. Reserve aux ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nombre de comptes credites"),
            @ApiResponse(responseCode = "403", description = "Role ADMIN requis")
    })
    public ResponseEntity<JobResultResponse> capitalizeInterest(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization) {
        guard.requireAdmin(authService.authenticate(RequestAuth.bearer(authorization)));
        return ResponseEntity.ok(new JobResultResponse(interestService.capitalizeSavings()));
    }

    @PostMapping("/overdue")
    @Operation(summary = "Marquer les prets en retard",
            description = "Recalcule le marqueur de retard de tous les prets. Reserve aux ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nombre de prets en retard"),
            @ApiResponse(responseCode = "403", description = "Role ADMIN requis")
    })
    public ResponseEntity<JobResultResponse> flagOverdue(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization) {
        guard.requireAdmin(authService.authenticate(RequestAuth.bearer(authorization)));
        return ResponseEntity.ok(new JobResultResponse(loanService.flagOverdueLoans()));
    }
}
