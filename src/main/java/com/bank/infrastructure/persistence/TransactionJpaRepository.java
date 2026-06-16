package com.bank.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionJpaRepository extends JpaRepository<TransactionJpa, String> {
    List<TransactionJpa> findByAccountId(String accountId);
}
