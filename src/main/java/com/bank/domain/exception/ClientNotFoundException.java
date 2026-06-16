package com.bank.domain.exception;

public class ClientNotFoundException extends BankException {
    public ClientNotFoundException(String id) {
        super("client not found: " + id);
    }

    @Override
    public String code() {
        return "CLIENT_NOT_FOUND";
    }

    @Override
    public int httpStatus() {
        return 404;
    }
}
