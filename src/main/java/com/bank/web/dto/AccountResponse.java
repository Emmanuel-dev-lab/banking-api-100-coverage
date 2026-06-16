package com.bank.web.dto;

import com.bank.domain.model.Account;

public record AccountResponse(String id, String clientId, String type, long balance, String status) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.id(),
                account.clientId(),
                account.type().name(),
                account.balance().amount(),
                account.status().name());
    }
}
