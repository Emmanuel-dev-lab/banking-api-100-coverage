package com.bank.domain.model;

/** Parametres de pagination valides : page >= 0, 1 <= size <= MAX_SIZE. */
public record PageRequest(int page, int size) {

    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1");
        }
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be <= " + MAX_SIZE);
        }
    }

    public int offset() {
        return page * size;
    }
}
