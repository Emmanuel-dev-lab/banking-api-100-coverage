package com.bank.domain.exception;

public class InvalidLoanTermsException extends BankException {
    public InvalidLoanTermsException(String reason) {
        super("invalid loan terms: " + reason);
    }

    @Override
    public String code() {
        return "INVALID_LOAN_TERMS";
    }

    @Override
    public int httpStatus() {
        return 400;
    }
}
