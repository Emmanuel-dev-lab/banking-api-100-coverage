package com.bank.domain.model;

/** Compte epargne : aucun decouvert, porte un taux d'interet annuel. */
public class SavingsAccount extends Account {

    private final double annualRate;

    public SavingsAccount(String id, String clientId, Money balance, double annualRate) {
        super(id, clientId, AccountType.SAVINGS, balance);
        this.annualRate = annualRate;
    }

    /** Reconstitution depuis la persistance. */
    public SavingsAccount(String id, String clientId, Money balance, AccountStatus status, double annualRate) {
        super(id, clientId, AccountType.SAVINGS, balance, status);
        this.annualRate = annualRate;
    }

    @Override
    protected boolean canWithdraw(Money amount) {
        return balance().amount() - amount.amount() >= 0;
    }

    /** Capitalise les interets (arrondi inferieur). Ne credite que si > 0. */
    public void applyInterest() {
        long interest = (long) Math.floor(balance().amount() * annualRate);
        if (interest > 0) {
            deposit(Money.of(interest));
        }
    }

    public double annualRate() {
        return annualRate;
    }
}
