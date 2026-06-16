package com.bank.domain.port;

import com.bank.domain.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    void save(Account account);

    Optional<Account> findById(String id);

    /**
     * Charge un compte en vue d'une mutation, avec verrou exclusif (anti
     * lost-update). A utiliser dans toute operation qui lit puis ecrit un solde.
     */
    Optional<Account> findByIdForUpdate(String id);

    List<Account> findAll(int offset, int limit);

    long count();

    List<Account> findByClientId(String clientId, int offset, int limit);

    long countByClientId(String clientId);
}
