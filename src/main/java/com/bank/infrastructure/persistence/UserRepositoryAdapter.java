package com.bank.infrastructure.persistence;

import com.bank.domain.model.User;
import com.bank.domain.port.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(User user) {
        jpa.save(new UserJpa(user.username(), user.id(), user.passwordHash(), user.role(), user.clientId()));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findById(username).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(String userId) {
        return jpa.findByUserId(userId).map(this::toDomain);
    }

    private User toDomain(UserJpa e) {
        return new User(e.getUserId(), e.getUsername(), e.getPasswordHash(), e.getRole(), e.getClientId());
    }
}
