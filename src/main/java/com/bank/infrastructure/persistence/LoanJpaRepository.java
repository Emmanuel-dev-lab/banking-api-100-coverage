package com.bank.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanJpaRepository extends JpaRepository<LoanJpa, String> {
}
