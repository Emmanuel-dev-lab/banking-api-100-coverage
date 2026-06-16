package com.bank.application.service;

import com.bank.domain.exception.ClientNotFoundException;
import com.bank.domain.model.Client;
import com.bank.domain.model.Page;
import com.bank.domain.model.PageRequest;
import com.bank.domain.model.Role;
import com.bank.domain.model.User;
import com.bank.domain.port.ClientRepository;
import com.bank.domain.port.IdGenerator;
import com.bank.domain.port.PasswordHasher;
import com.bank.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final IdGenerator idGenerator;
    private final PasswordHasher passwordHasher;

    public ClientService(ClientRepository clientRepository, UserRepository userRepository,
                         IdGenerator idGenerator, PasswordHasher passwordHasher) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public Client createClient(String firstName, String lastName, String username, String rawPassword) {
        String clientId = idGenerator.newId();
        Client client = new Client(clientId, firstName, lastName);
        clientRepository.save(client);
        User user = new User(idGenerator.newId(), username, passwordHasher.hash(rawPassword), Role.CLIENT, clientId);
        userRepository.save(user);
        return client;
    }

    public Client getClient(String id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }

    @Transactional
    public Client updateClient(String id, String firstName, String lastName) {
        getClient(id); // 404 si inconnu
        Client updated = new Client(id, firstName, lastName);
        clientRepository.save(updated);
        return updated;
    }

    public Page<Client> listClients(int page, int size) {
        PageRequest pr = new PageRequest(page, size);
        return new Page<>(clientRepository.findAll(pr.offset(), pr.size()),
                clientRepository.count(), pr.page(), pr.size());
    }
}
