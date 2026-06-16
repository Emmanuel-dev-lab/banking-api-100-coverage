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
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/loans")
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
    public ResponseEntity<LoanResponse> create(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody CreateLoanRequest request) {
        TokenClaims claims = authService.authenticate(RequestAuth.bearer(authorization));
        guard.requireOwnerOrAdmin(claims, request.clientId());
        Loan loan = loanService.requestLoan(request.clientId(), request.accountId(),
                request.principal(), request.annualRate(), request.termMonths());
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanResponse.from(loan));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<List<InstallmentResponse>> schedule(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String id) {
        Loan loan = authorize(authorization, id);
        List<InstallmentResponse> schedule = loan.schedule().stream()
                .map(InstallmentResponse::from)
                .toList();
        return ResponseEntity.ok(schedule);
    }

    @PostMapping("/{id}/repay")
    public ResponseEntity<Void> repay(
            @RequestHeader(name = "Authorization", required = false) String authorization,
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
