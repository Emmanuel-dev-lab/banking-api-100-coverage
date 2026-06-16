package com.bank.infrastructure.persistence;

import com.bank.domain.model.Money;
import com.bank.domain.model.Transaction;
import com.bank.domain.port.TransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpa;

    public TransactionRepositoryAdapter(TransactionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Transaction transaction) {
        jpa.save(new TransactionJpa(transaction.id(), transaction.accountId(), transaction.type(),
                transaction.amount().amount(), transaction.date(), transaction.relatedAccountId()));
    }

    @Override
    public List<Transaction> findByAccountId(String accountId) {
        return jpa.findByAccountId(accountId).stream()
                .map(e -> new Transaction(e.getId(), e.getAccountId(), e.getType(),
                        Money.of(e.getAmount()), e.getDate(), e.getRelatedAccountId()))
                .toList();
    }
}
