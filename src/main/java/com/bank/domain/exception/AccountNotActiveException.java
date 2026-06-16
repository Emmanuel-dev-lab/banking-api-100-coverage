package com.bank.domain.exception;

public class AccountNotActiveException extends BankException {
    public AccountNotActiveException(String accountId) {
        super("account not active: " + accountId);
    }

    @Override
    public String code() {
        return "ACCOUNT_NOT_ACTIVE";
    }

    @Override
    public int httpStatus() {
        return 409;
    }
}
