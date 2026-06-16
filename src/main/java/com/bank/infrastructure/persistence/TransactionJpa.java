package com.bank.infrastructure.persistence;

import com.bank.domain.model.TransactionType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class TransactionJpa {

    @Id
    private String id;
    private String accountId;
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    private long amount;
    private LocalDate date;
    private String relatedAccountId;

    protected TransactionJpa() {
    }

    public TransactionJpa(String id, String accountId, TransactionType type, long amount,
                          LocalDate date, String relatedAccountId) {
        this.id = id;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.relatedAccountId = relatedAccountId;
    }

    public String getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public long getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getRelatedAccountId() {
        return relatedAccountId;
    }
}
