package com.bank.domain.exception;

public class UnauthorizedException extends BankException {
    public UnauthorizedException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return "UNAUTHORIZED";
    }

    @Override
    public int httpStatus() {
        return 401;
    }
}
