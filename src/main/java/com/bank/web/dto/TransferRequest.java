package com.bank.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
        @NotBlank(message = "sourceAccountId is required") String sourceAccountId,
        @NotBlank(message = "destAccountId is required") String destAccountId,
        @Positive(message = "amount must be > 0") long amount) {
}
