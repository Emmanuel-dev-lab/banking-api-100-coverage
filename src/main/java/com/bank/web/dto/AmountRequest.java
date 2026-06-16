package com.bank.web.dto;

import jakarta.validation.constraints.Positive;

public record AmountRequest(@Positive(message = "amount must be > 0") long amount) {
}
