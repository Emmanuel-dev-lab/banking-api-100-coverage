package com.bank.domain.model;

import java.time.LocalDate;

/** Une echeance de l'echeancier d'amortissement. */
public class Installment {

    private final int index;
    private final LocalDate dueDate;
    private final Money amount;
    private final Money principalPart;
    private final Money interestPart;
    private boolean paid;

    public Installment(int index, LocalDate dueDate, Money amount, Money principalPart, Money interestPart) {
        this(index, dueDate, amount, principalPart, interestPart, false);
    }

    /** Reconstitution depuis la persistance (avec etat de paiement). */
    public Installment(int index, LocalDate dueDate, Money amount, Money principalPart,
                       Money interestPart, boolean paid) {
        this.index = index;
        this.dueDate = dueDate;
        this.amount = amount;
        this.principalPart = principalPart;
        this.interestPart = interestPart;
        this.paid = paid;
    }

    public void markPaid() {
        paid = true;
    }

    public int index() {
        return index;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public Money amount() {
        return amount;
    }

    public Money principalPart() {
        return principalPart;
    }

    public Money interestPart() {
        return interestPart;
    }

    public boolean paid() {
        return paid;
    }
}
