package com.bank.web.dto;

import com.bank.domain.model.Transaction;

public record TransactionResponse(
        String id, String accountId, String type, long amount, String date, String relatedAccountId) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.id(),
                transaction.accountId(),
                transaction.type().name(),
                transaction.amount().amount(),
                transaction.date().toString(),
                transaction.relatedAccountId());
    }
}
