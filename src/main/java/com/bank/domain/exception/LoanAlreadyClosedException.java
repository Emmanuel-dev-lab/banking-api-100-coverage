package com.bank.domain.exception;

public class LoanAlreadyClosedException extends BankException {
    public LoanAlreadyClosedException(String loanId) {
        super("loan already paid off: " + loanId);
    }

    @Override
    public String code() {
        return "LOAN_ALREADY_CLOSED";
    }

    @Override
    public int httpStatus() {
        return 409;
    }
}
