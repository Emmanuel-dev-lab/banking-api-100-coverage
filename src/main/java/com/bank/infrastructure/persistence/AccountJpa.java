package com.bank.infrastructure.persistence;

import com.bank.domain.model.AccountStatus;
import com.bank.domain.model.AccountType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class AccountJpa {

    @Id
    private String id;
    private String clientId;
    @Enumerated(EnumType.STRING)
    private AccountType type;
    private long balance;
    @Enumerated(EnumType.STRING)
    private AccountStatus status;
    private Long overdraftLimit;
    private Double annualRate;

    protected AccountJpa() {
    }

    public AccountJpa(String id, String clientId, AccountType type, long balance,
                      AccountStatus status, Long overdraftLimit, Double annualRate) {
        this.id = id;
        this.clientId = clientId;
        this.type = type;
        this.balance = balance;
        this.status = status;
        this.overdraftLimit = overdraftLimit;
        this.annualRate = annualRate;
    }

    public String getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public AccountType getType() {
        return type;
    }

    public long getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Long getOverdraftLimit() {
        return overdraftLimit;
    }

    public Double getAnnualRate() {
        return annualRate;
    }
}
