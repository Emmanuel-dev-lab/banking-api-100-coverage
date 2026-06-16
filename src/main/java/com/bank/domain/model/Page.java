package com.bank.domain.model;

import java.util.List;

/**
 * Resultat pagine. {@code size} provient toujours d'un {@link PageRequest}
 * (>= 1), donc {@link #totalPages()} ne contient aucune branche defensive.
 */
public record Page<T>(List<T> content, long totalElements, int page, int size) {

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / (double) size);
    }
}
