package com.bank.domain.exception;

public class LoanNotFoundException extends BankException {
    public LoanNotFoundException(String id) {
        super("loan not found: " + id);
    }

    @Override
    public String code() {
        return "LOAN_NOT_FOUND";
    }

    @Override
    public int httpStatus() {
        return 404;
    }
}
