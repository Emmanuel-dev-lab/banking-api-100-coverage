package com.bank.web.dto;

/** Demande de pret pour le client courant : le clientId vient du jeton, pas du corps. */
public record MeCreateLoanRequest(String accountId, long principal, double annualRate, int termMonths) {
}
