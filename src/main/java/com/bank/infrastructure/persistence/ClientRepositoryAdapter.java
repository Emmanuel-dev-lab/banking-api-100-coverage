package com.bank.infrastructure.persistence;

import com.bank.domain.model.Client;
import com.bank.domain.port.ClientRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ClientRepositoryAdapter implements ClientRepository {

    private final ClientJpaRepository jpa;

    public ClientRepositoryAdapter(ClientJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Client client) {
        jpa.save(new ClientJpa(client.id(), client.firstName(), client.lastName()));
    }

    @Override
    public Optional<Client> findById(String id) {
        return jpa.findById(id)
                .map(e -> new Client(e.getId(), e.getFirstName(), e.getLastName()));
    }
}
