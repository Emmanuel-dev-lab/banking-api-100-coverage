package com.bank.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpa, String> {
    Optional<UserJpa> findByUserId(String userId);
}
