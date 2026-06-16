package com.bank.web.dto;

public record CreateLoanRequest(
        String clientId, String accountId, long principal, double annualRate, int termMonths) {
}
