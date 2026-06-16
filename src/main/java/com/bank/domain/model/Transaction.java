package com.bank.domain.model;

import java.time.LocalDate;

/** Ecriture de registre (historique). Immuable, sans logique branchante. */
public record Transaction(
        String id,
        String accountId,
        TransactionType type,
        Money amount,
        LocalDate date,
        String relatedAccountId) {
}
