package com.bank.domain.port;

import com.bank.domain.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    void save(Account account);

    Optional<Account> findById(String id);

    List<Account> findAll(int offset, int limit);

    long count();

    List<Account> findByClientId(String clientId, int offset, int limit);

    long countByClientId(String clientId);
}
