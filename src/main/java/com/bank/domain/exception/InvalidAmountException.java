package com.bank.domain.exception;

public class InvalidAmountException extends BankException {
    public InvalidAmountException(long amount) {
        super("amount must be strictly positive: " + amount);
    }

    @Override
    public String code() {
        return "INVALID_AMOUNT";
    }

    @Override
    public int httpStatus() {
        return 400;
    }
}
