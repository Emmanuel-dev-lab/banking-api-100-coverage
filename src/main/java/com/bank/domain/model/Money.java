package com.bank.domain.model;

import com.bank.domain.exception.InvalidAmountException;

/**
 * Montant monetaire en XAF (Franc CFA, 0 decimale -> unites entieres).
 * Immuable. Un montant peut etre negatif (resultat d'une soustraction),
 * mais les fabriques {@link #of} et {@link #ofPositive} contraignent les entrees.
 */
public record Money(long amount) {

    public static final Money ZERO = new Money(0);

    /** Cree un montant >= 0. */
    public static Money of(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0: " + amount);
        }
        return new Money(amount);
    }

    /**
     * Reconstitue un montant deja calcule/persiste, sans contrainte de signe.
     * A utiliser pour relire un solde (potentiellement negatif : decouvert) ou
     * un capital restant. Ne pas utiliser pour valider une entree utilisateur.
     */
    public static Money fromStored(long amount) {
        return new Money(amount);
    }

    /** Cree un montant strictement positif (operation d'argent). */
    public static Money ofPositive(long amount) {
        if (amount <= 0) {
            throw new InvalidAmountException(amount);
        }
        return new Money(amount);
    }

    public Money plus(Money other) {
        return new Money(amount + other.amount);
    }

    public Money minus(Money other) {
        return new Money(amount - other.amount);
    }

    public boolean isNegative() {
        return amount < 0;
    }

    public boolean isGreaterThan(Money other) {
        return amount > other.amount;
    }

    public boolean isLessThan(Money other) {
        return amount < other.amount;
    }
}
