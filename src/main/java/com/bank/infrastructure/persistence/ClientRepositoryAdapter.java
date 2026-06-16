package com.bank.infrastructure.persistence;

import com.bank.domain.model.Client;
import com.bank.domain.port.ClientRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
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
                .map(this::toDomain);
    }

    @Override
    public List<Client> findAll(int offset, int limit) {
        return jpa.findAll(PageRequest.of(offset / limit, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpa.count();
    }

    private Client toDomain(ClientJpa e) {
        return new Client(e.getId(), e.getFirstName(), e.getLastName());
    }
}
