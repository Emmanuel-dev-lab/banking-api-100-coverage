package com.bank.web.dto;

import com.bank.domain.model.AccountType;

public record OpenAccountRequest(AccountType type, long overdraftLimit, double annualRate) {
}
