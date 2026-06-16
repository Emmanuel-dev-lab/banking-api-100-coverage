package com.bank.domain.model;

import com.bank.domain.exception.AccountNotActiveException;
import com.bank.domain.exception.InsufficientFundsException;

/**
 * Agregat compte. La regle de decouvert est specialisee par sous-type
 * via {@link #canWithdraw(Money)}. Les operations valident l'etat avant de muter.
 */
public abstract class Account {

    private final String id;
    private final String clientId;
    private final AccountType type;
    private Money balance;
    private AccountStatus status;

    protected Account(String id, String clientId, AccountType type, Money balance) {
        this(id, clientId, type, balance, AccountStatus.ACTIVE);
    }

    /** Constructeur de reconstitution (persistance) : restaure le statut. */
    protected Account(String id, String clientId, AccountType type, Money balance, AccountStatus status) {
        this.id = id;
        this.clientId = clientId;
        this.type = type;
        this.balance = balance;
        this.status = status;
    }

    public void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(id);
        }
    }

    public void deposit(Money amount) {
        ensureActive();
        balance = balance.plus(amount);
    }

    public void withdraw(Money amount) {
        ensureActive();
        if (!canWithdraw(amount)) {
            throw new InsufficientFundsException(id);
        }
        balance = balance.minus(amount);
    }

    protected abstract boolean canWithdraw(Money amount);

    public void freeze() {
        status = AccountStatus.FROZEN;
    }

    public void close() {
        if (balance.amount() != 0) {
            throw new IllegalStateException("account not settled: " + id);
        }
        status = AccountStatus.CLOSED;
    }

    /** Reactive un compte gele. Un compte ferme ne peut pas etre reactive. */
    public void reactivate() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("account closed: " + id);
        }
        status = AccountStatus.ACTIVE;
    }

    public String id() {
        return id;
    }

    public String clientId() {
        return clientId;
    }

    public AccountType type() {
        return type;
    }

    public Money balance() {
        return balance;
    }

    public AccountStatus status() {
        return status;
    }
}
