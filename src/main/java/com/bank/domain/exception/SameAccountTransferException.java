package com.bank.domain.exception;

public class SameAccountTransferException extends BankException {
    public SameAccountTransferException(String accountId) {
        super("source and destination accounts are identical: " + accountId);
    }

    @Override
    public String code() {
        return "SAME_ACCOUNT_TRANSFER";
    }

    @Override
    public int httpStatus() {
        return 422;
    }
}
