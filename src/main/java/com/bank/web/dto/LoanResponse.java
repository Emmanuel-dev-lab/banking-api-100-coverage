package com.bank.web.dto;

import com.bank.domain.model.Loan;

import java.util.List;

public record LoanResponse(
        String id,
        String clientId,
        String accountId,
        long principal,
        long outstandingPrincipal,
        String status,
        List<InstallmentResponse> schedule) {

    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.id(),
                loan.clientId(),
                loan.accountId(),
                loan.principal().amount(),
                loan.outstandingPrincipal().amount(),
                loan.status().name(),
                loan.schedule().stream().map(InstallmentResponse::from).toList());
    }
}
