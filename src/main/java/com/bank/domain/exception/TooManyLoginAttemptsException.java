package com.bank.domain.exception;

public class TooManyLoginAttemptsException extends BankException {
    public TooManyLoginAttemptsException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return "TOO_MANY_ATTEMPTS";
    }

    @Override
    public int httpStatus() {
        return 429;
    }
}
