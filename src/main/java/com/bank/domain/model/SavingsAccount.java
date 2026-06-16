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

    /**
     * Capitalise les interets d'un mois : {@code floor(solde * tauxAnnuel/12)}
     * (arrondi inferieur). Ne credite que si le montant est strictement positif.
     * Renvoie le montant credite (0 si aucun).
     */
    public Money applyMonthlyInterest() {
        long interest = (long) Math.floor(balance().amount() * annualRate / 12.0);
        if (interest > 0) {
            Money credited = Money.of(interest);
            deposit(credited);
            return credited;
        }
        return Money.ZERO;
    }

    public double annualRate() {
        return annualRate;
    }
}
