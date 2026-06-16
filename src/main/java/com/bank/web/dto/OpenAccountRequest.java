package com.bank.web.dto;

import com.bank.domain.model.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record OpenAccountRequest(
        @NotNull(message = "type is required") AccountType type,
        @PositiveOrZero(message = "overdraftLimit must be >= 0") long overdraftLimit,
        @PositiveOrZero(message = "annualRate must be >= 0") double annualRate) {
}
