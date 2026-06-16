package com.bank.domain.model;

/** Compte courant : decouvert autorise jusqu'a {@code overdraftLimit}. */
public class CurrentAccount extends Account {

    private final long overdraftLimit;

    public CurrentAccount(String id, String clientId, Money balance, long overdraftLimit) {
        super(id, clientId, AccountType.CURRENT, balance);
        this.overdraftLimit = overdraftLimit;
    }

    /** Reconstitution depuis la persistance. */
    public CurrentAccount(String id, String clientId, Money balance, AccountStatus status, long overdraftLimit) {
        super(id, clientId, AccountType.CURRENT, balance, status);
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    protected boolean canWithdraw(Money amount) {
        long after = balance().amount() - amount.amount();
        return after >= -overdraftLimit;
    }

    public long overdraftLimit() {
        return overdraftLimit;
    }
}
