package com.bank.web.dto;

import com.bank.domain.model.Loan;

import java.util.List;

public record LoanResponse(String id, List<InstallmentResponse> schedule) {
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.id(),
                loan.schedule().stream().map(InstallmentResponse::from).toList());
    }
}
