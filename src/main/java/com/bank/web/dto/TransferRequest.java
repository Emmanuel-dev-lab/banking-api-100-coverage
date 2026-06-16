package com.bank.web.dto;

public record TransferRequest(String sourceAccountId, String destAccountId, long amount) {
}
