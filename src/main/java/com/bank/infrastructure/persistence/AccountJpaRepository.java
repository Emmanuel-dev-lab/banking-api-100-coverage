package com.bank.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountJpaRepository extends JpaRepository<AccountJpa, String> {
    List<AccountJpa> findByClientId(String clientId, Pageable pageable);

    long countByClientId(String clientId);
}
