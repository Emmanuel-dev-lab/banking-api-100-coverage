package com.bank.domain.exception;

public class InsufficientFundsException extends BankException {
    public InsufficientFundsException(String accountId) {
        super("insufficient funds on account: " + accountId);
    }

    @Override
    public String code() {
        return "INSUFFICIENT_FUNDS";
    }

    @Override
    public int httpStatus() {
        return 422;
    }
}
