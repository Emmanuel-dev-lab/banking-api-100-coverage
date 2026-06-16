package com.bank.domain.exception;

/**
 * Racine des exceptions metier. Chaque sous-type expose un {@link #code()}
 * stable et un {@link #httpStatus()} constant -> aucune branche dans le mapping HTTP.
 */
public abstract class BankException extends RuntimeException {

    protected BankException(String message) {
        super(message);
    }

    public abstract String code();

    public abstract int httpStatus();
}
