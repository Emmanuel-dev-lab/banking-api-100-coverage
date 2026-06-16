package com.bank.domain.port;

import com.bank.domain.model.Transaction;

import java.util.List;

public interface TransactionRepository {
    void save(Transaction transaction);

    /** Page d'ecritures du compte, des plus recentes aux plus anciennes. */
    List<Transaction> findByAccountId(String accountId, int offset, int limit);

    long countByAccountId(String accountId);
}
