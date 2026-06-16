package com.bank.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Demande de pret pour le client courant : le clientId vient du jeton, pas du corps. */
public record MeCreateLoanRequest(
        @NotBlank(message = "accountId is required") String accountId,
        @Positive(message = "principal must be > 0") long principal,
        @PositiveOrZero(message = "annualRate must be >= 0") double annualRate,
        @Positive(message = "termMonths must be > 0") int termMonths) {
}
