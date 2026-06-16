package com.bank.infrastructure.persistence;

import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public class InstallmentEmb {

    private int idx;
    private LocalDate dueDate;
    private long amount;
    private long principalPart;
    private long interestPart;
    private boolean paid;

    protected InstallmentEmb() {
    }

    public InstallmentEmb(int idx, LocalDate dueDate, long amount, long principalPart,
                          long interestPart, boolean paid) {
        this.idx = idx;
        this.dueDate = dueDate;
        this.amount = amount;
        this.principalPart = principalPart;
        this.interestPart = interestPart;
        this.paid = paid;
    }

    public int getIdx() {
        return idx;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public long getAmount() {
        return amount;
    }

    public long getPrincipalPart() {
        return principalPart;
    }

    public long getInterestPart() {
        return interestPart;
    }

    public boolean isPaid() {
        return paid;
    }
}
