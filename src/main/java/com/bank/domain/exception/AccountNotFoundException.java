package com.bank.domain.exception;

public class AccountNotFoundException extends BankException {
    public AccountNotFoundException(String id) {
        super("account not found: " + id);
    }

    @Override
    public String code() {
        return "ACCOUNT_NOT_FOUND";
    }

    @Override
    public int httpStatus() {
        return 404;
    }
}
