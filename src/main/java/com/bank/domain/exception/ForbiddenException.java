package com.bank.domain.exception;

public class ForbiddenException extends BankException {
    public ForbiddenException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return "FORBIDDEN";
    }

    @Override
    public int httpStatus() {
        return 403;
    }
}
