package com.bank.domain.port;

import com.bank.domain.model.Account;

import java.util.Optional;

public interface AccountRepository {
    void save(Account account);

    Optional<Account> findById(String id);
}
