package com.bank.domain.port;

import com.bank.domain.model.Client;

import java.util.Optional;

public interface ClientRepository {
    void save(Client client);

    Optional<Client> findById(String id);
}
