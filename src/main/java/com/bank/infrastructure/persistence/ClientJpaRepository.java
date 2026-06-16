package com.bank.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientJpaRepository extends JpaRepository<ClientJpa, String> {
}
