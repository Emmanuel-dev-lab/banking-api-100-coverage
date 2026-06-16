package com.bank.domain.port;

import com.bank.domain.model.Transaction;

import java.util.List;

public interface TransactionRepository {
    void save(Transaction transaction);

    List<Transaction> findByAccountId(String accountId);
}
